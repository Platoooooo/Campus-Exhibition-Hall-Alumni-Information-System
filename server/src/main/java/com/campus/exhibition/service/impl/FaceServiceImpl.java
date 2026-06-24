package com.campus.exhibition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.exhibition.common.BizException;
import com.campus.exhibition.dto.face.*;
import com.campus.exhibition.entity.*;
import com.campus.exhibition.enums.ArchiveStatus;
import com.campus.exhibition.mapper.*;
import com.campus.exhibition.service.FaceService;
import com.campus.exhibition.vo.FaceEnrollVO;
import com.campus.exhibition.vo.FaceRecognizeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 人脸识别服务实现
 * - 特征加密存储（AES，不使用打印人脸数据）
 * - 可配置不保留原图
 * - 日志不含人脸原始数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FaceServiceImpl implements FaceService {

    private final RestTemplate restTemplate;
    private final FaceFeatureMapper faceFeatureMapper;
    private final FaceRecogLogMapper recogLogMapper;
    private final AlumniMapper alumniMapper;
    private final ArchiveMapper archiveMapper;
    private final ArchiveMediaMapper mediaMapper;
    private final SysCollegeMapper collegeMapper;
    private final ArchiveCategoryMapper categoryMapper;

    @Value("${app.face.service-url:http://localhost:8000}")
    private String faceServiceUrl;

    @Value("${app.face.quality-threshold:0.6}")
    private float qualityThreshold;

    /** 特征加密密钥（部署时通过环境变量覆盖） */
    @Value("${app.face.encrypt-key:change-me-in-production}")
    private String encryptKey;

    /** 是否保留上传原图 */
    @Value("${app.face.store-original-image:false}")
    private boolean storeOriginalImage;

    // ── 录入 ────────────────────────────────────────────

    @Override
    @Transactional
    public FaceEnrollVO enroll(Long alumniId, MultipartFile file) {
        Alumni alumni = alumniMapper.selectById(alumniId);
        if (alumni == null) throw new BizException(400, "校友不存在");

        // 图片转 base64（不记录日志——隐私）
        String base64;
        try {
            base64 = Base64.getEncoder().encodeToString(file.getBytes());
        } catch (IOException e) {
            throw new BizException(400, "图片读取失败");
        }

        // 调 face-service /extract
        FaceExtractResponse extractResp = callExtract(base64);

        FaceExtractResponse.ExtractData data = extractResp.getData();
        if (!data.isFaceFound()) {
            throw new BizException(5101, "未检测到清晰人脸，请重新采集");
        }
        if (data.getFaceCount() > 1) {
            throw new BizException(5103, "检测到多张人脸，请单人采集");
        }
        if (data.getQuality() != null && data.getQuality() < qualityThreshold) {
            throw new BizException(5102,
                    "人脸质量不达标（当前 " + String.format("%.2f", data.getQuality())
                  + "，门槛 " + String.format("%.2f", qualityThreshold) + "）");
        }

        // 特征 float[] → byte[] → AES 加密
        byte[] rawBytes = floatsToBytes(data.getFeature());
        byte[] encrypted = encrypt(rawBytes);

        // 保存/更新 face_feature
        FaceFeature existing = faceFeatureMapper.selectOne(
                new LambdaQueryWrapper<FaceFeature>().eq(FaceFeature::getAlumniId, alumniId));
        if (existing != null) {
            existing.setFeature(encrypted);
            existing.setModelVer(data.getModelVer());
            existing.setQuality(data.getQuality());
            existing.setStatus(1);
            faceFeatureMapper.updateById(existing);
        } else {
            FaceFeature ff = new FaceFeature();
            ff.setAlumniId(alumniId);
            ff.setFeature(encrypted);
            ff.setModelVer(data.getModelVer());
            ff.setQuality(data.getQuality());
            ff.setStatus(1);
            faceFeatureMapper.insert(ff);
        }

        // 更新校友 face_status
        alumni.setFaceStatus(1);
        alumniMapper.updateById(alumni);

        // 不保留原图（可配置）
        if (!storeOriginalImage) {
            // 原图 base64 对象已在本方法栈内，方法结束即释放，不做持久化
            log.debug("原图未保留（storeOriginalImage=false）");
        }

        log.info("人脸录入成功: alumniId={}, quality={}, modelVer={}",
                alumniId, data.getQuality(), data.getModelVer());

        return FaceEnrollVO.builder()
                .alumniId(alumniId)
                .quality(data.getQuality())
                .modelVer(data.getModelVer())
                .faceStatus(1).build();
    }

    // ── 识别 ────────────────────────────────────────────

    @Override
    public FaceRecognizeVO recognize(FaceRecognizeRequest request) {
        long startMs = System.currentTimeMillis();

        // 查全部有效特征（解密后作为 candidates）
        List<FaceFeature> features;
        try {
            features = faceFeatureMapper.selectList(
                    new LambdaQueryWrapper<FaceFeature>().eq(FaceFeature::getStatus, 1));
        } catch (Exception e) {
            log.error("查询底库失败", e);
            return degraded(null, request.getDevice(), startMs);
        }

        if (features.isEmpty()) {
            // 底库为空 → NO_MATCH（不是降级，是正常场景）
            log.info("底库为空，无法识别");
            writeRecogLog(null, null, false, request.getDevice(),
                    System.currentTimeMillis() - startMs);
            return FaceRecognizeVO.builder()
                    .status("NO_MATCH").alumniId(null).score(null)
                    .alumni(null).timeline(List.of()).build();
        }

        // 构建 /match 请求（解密特征）
        List<FaceMatchRequest.Candidate> candidates;
        try {
            candidates = features.stream()
                    .map(f -> FaceMatchRequest.Candidate.builder()
                            .alumniId(f.getAlumniId())
                            .feature(bytesToFloats(decrypt(f.getFeature())))
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("特征解密失败", e);
            return degraded(null, request.getDevice(), startMs);
        }

        FaceMatchRequest matchReq = FaceMatchRequest.builder()
                .image(request.getImage())
                .threshold(0.80)  // 与 face-service 默认阈值一致（生活照模式）
                .candidates(candidates)
                .build();

        // 调 face-service /match
        FaceMatchResponse matchResp;
        try {
            matchResp = restTemplate.postForObject(
                    faceServiceUrl + "/match", matchReq, FaceMatchResponse.class);
        } catch (RestClientException e) {
            log.warn("face-service /match 调用失败（连接异常或超时，降级处理）");
            return degraded(null, request.getDevice(), startMs);
        }

        if (matchResp == null || matchResp.getCode() != 0) {
            log.warn("face-service /match 返回异常 code={}", matchResp != null ? matchResp.getCode() : "null");
            return degraded(null, request.getDevice(), startMs);
        }

        FaceMatchResponse.MatchData data = matchResp.getData();

        long costMs = System.currentTimeMillis() - startMs;
        writeRecogLog(data.getAlumniId(), data.getScore(), data.isHit(),
                request.getDevice(), costMs);

        if (!data.isHit()) {
            log.info("识别未命中: device={}, score={}, costMs={}",
                    request.getDevice(), data.getScore(), costMs);
            return FaceRecognizeVO.builder()
                    .status("NO_MATCH").alumniId(null).score(data.getScore())
                    .alumni(null).timeline(List.of()).build();
        }

        // 命中：查校友 + 成长轨迹（仅 published）
        Alumni alumni = alumniMapper.selectById(data.getAlumniId());
        if (alumni == null) {
            log.warn("命中校友不存在: alumniId={}", data.getAlumniId());
            return FaceRecognizeVO.builder()
                    .status("NO_MATCH").alumniId(null).score(null)
                    .alumni(null).timeline(List.of()).build();
        }

        log.info("识别命中: alumniId={}, name={}, score={}, costMs={}",
                data.getAlumniId(), alumni.getName(), data.getScore(), costMs);

        FaceRecognizeVO.AlumniBrief brief = buildAlumniBrief(alumni);
        List<FaceRecognizeVO.TimelineItem> timeline = buildTimeline(data.getAlumniId());

        return FaceRecognizeVO.builder()
                .status("HIT").alumniId(data.getAlumniId()).score(data.getScore())
                .alumni(brief).timeline(timeline).build();
    }

    // ── face-service HTTP 调用 ──────────────────────────

    private FaceExtractResponse callExtract(String base64) {
        try {
            Map<String, Object> req = new LinkedHashMap<>();
            req.put("image", base64);
            req.put("needQuality", true);
            return restTemplate.postForObject(
                    faceServiceUrl + "/extract", req, FaceExtractResponse.class);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.warn("face-service /extract 返回客户端错误: status={}", e.getStatusCode());
            try {
                var errNode = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readTree(e.getResponseBodyAsString());
                int errCode = errNode.path("detail").path("code").asInt(5104);
                String errMsg = errNode.path("detail").path("message").asText("face-service 异常");
                throw new BizException(errCode, errMsg);
            } catch (BizException be) { throw be; }
            catch (Exception ex) { throw new BizException(5104, "face-service 异常"); }
        } catch (RestClientException e) {
            log.warn("face-service /extract 调用失败（网络/超时，face-service 未启动）");
            throw new BizException(5104, "face-service 不可用");
        }
    }

    private FaceRecognizeVO degraded(Float score, String device, long startMs) {
        writeRecogLog(null, score, false, device,
                System.currentTimeMillis() - startMs);
        return FaceRecognizeVO.builder()
                .status("DEGRADED").alumniId(null).score(null)
                .alumni(null).timeline(List.of()).build();
    }

    // ── 特征加密/解密 ───────────────────────────────────

    /**
     * AES 加密特征向量。
     * 密钥通过 app.face.encrypt-key 配置。
     */
    private byte[] encrypt(byte[] data) {
        try {
            SecretKeySpec keySpec = deriveKey();
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            log.error("特征加密失败", e);
            throw new BizException(5104, "特征加密失败");
        }
    }

    /**
     * AES 解密特征向量
     */
    private byte[] decrypt(byte[] data) {
        try {
            SecretKeySpec keySpec = deriveKey();
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            log.error("特征解密失败", e);
            throw new BizException(5104, "特征解密失败");
        }
    }

    /** 由配置密钥派生 128-bit AES 密钥 */
    private SecretKeySpec deriveKey() throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] hash = sha.digest(encryptKey.getBytes(StandardCharsets.UTF_8));
        // AES-128: 取前 16 字节
        return new SecretKeySpec(Arrays.copyOf(hash, 16), "AES");
    }

    // ── 辅助方法 ────────────────────────────────────────

    private FaceRecognizeVO.AlumniBrief buildAlumniBrief(Alumni a) {
        SysCollege c = collegeMapper.selectById(a.getCollegeId());
        return FaceRecognizeVO.AlumniBrief.builder()
                .id(a.getId()).name(a.getName()).avatar(a.getAvatar())
                .collegeName(c != null ? c.getName() : null)
                .gradYear(a.getGradYear()).summary(a.getSummary()).build();
    }

    private List<FaceRecognizeVO.TimelineItem> buildTimeline(Long alumniId) {
        List<Archive> archives = archiveMapper.selectList(
                new LambdaQueryWrapper<Archive>()
                        .eq(Archive::getAlumniId, alumniId)
                        .eq(Archive::getStatus, ArchiveStatus.PUBLISHED.getCode())
                        .orderByAsc(Archive::getEventDate));

        return archives.stream().map(a -> {
            ArchiveCategory cat = categoryMapper.selectById(a.getCategoryId());
            List<FaceRecognizeVO.MediaBrief> media = mediaMapper.selectList(
                    new LambdaQueryWrapper<ArchiveMedia>()
                            .eq(ArchiveMedia::getArchiveId, a.getId())
                            .orderByAsc(ArchiveMedia::getSort))
                    .stream().map(m -> FaceRecognizeVO.MediaBrief.builder()
                            .type(m.getType()).url(m.getUrl())
                            .thumbnail(m.getThumbnail()).duration(m.getDuration()).build())
                    .collect(Collectors.toList());

            return FaceRecognizeVO.TimelineItem.builder()
                    .archiveId(a.getId())
                    .categoryName(cat != null ? cat.getName() : null)
                    .title(a.getTitle())
                    .eventDate(a.getEventDate() != null ? a.getEventDate().toString() : null)
                    .content(a.getContent())
                    .media(media).build();
        }).collect(Collectors.toList());
    }

    private void writeRecogLog(Long alumniId, Float score, boolean hit,
                               String device, long costMs) {
        FaceRecogLog logEntity = new FaceRecogLog();
        logEntity.setAlumniId(alumniId);
        logEntity.setScore(score);
        logEntity.setHit(hit ? 1 : 0);
        logEntity.setDevice(device);
        logEntity.setCostMs((int) costMs);
        recogLogMapper.insert(logEntity);
    }

    private byte[] floatsToBytes(List<Float> floats) {
        if (floats == null) return new byte[0];
        byte[] bytes = new byte[floats.size() * 4];
        for (int i = 0; i < floats.size(); i++) {
            int bits = Float.floatToIntBits(floats.get(i));
            bytes[i * 4]     = (byte) (bits >> 24);
            bytes[i * 4 + 1] = (byte) (bits >> 16);
            bytes[i * 4 + 2] = (byte) (bits >> 8);
            bytes[i * 4 + 3] = (byte) bits;
        }
        return bytes;
    }

    private List<Float> bytesToFloats(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return List.of();
        List<Float> floats = new ArrayList<>(bytes.length / 4);
        for (int i = 0; i < bytes.length; i += 4) {
            int bits = ((bytes[i] & 0xFF) << 24)
                     | ((bytes[i + 1] & 0xFF) << 16)
                     | ((bytes[i + 2] & 0xFF) << 8)
                     | (bytes[i + 3] & 0xFF);
            floats.add(Float.intBitsToFloat(bits));
        }
        return floats;
    }
}
