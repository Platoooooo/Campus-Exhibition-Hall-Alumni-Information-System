package com.campus.exhibition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.exhibition.common.BizException;
import com.campus.exhibition.dto.face.*;
import com.campus.exhibition.entity.*;
import com.campus.exhibition.mapper.*;
import com.campus.exhibition.vo.FaceEnrollVO;
import com.campus.exhibition.vo.FaceRecognizeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FaceServiceImpl 人脸服务单元测试
 * 策略：Mock RestTemplate + Mapper，反射测试加解密
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FaceService 人脸服务")
class FaceServiceImplTest {

    @Mock private RestTemplate restTemplate;
    @Mock private FaceFeatureMapper faceFeatureMapper;
    @Mock private FaceRecogLogMapper recogLogMapper;
    @Mock private AlumniMapper alumniMapper;
    @Mock private ArchiveMapper archiveMapper;
    @Mock private ArchiveMediaMapper mediaMapper;
    @Mock private SysCollegeMapper collegeMapper;
    @Mock private ArchiveCategoryMapper categoryMapper;
    @InjectMocks private FaceServiceImpl faceService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(faceService, "faceServiceUrl", "http://localhost:8000");
        ReflectionTestUtils.setField(faceService, "qualityThreshold", 0.6f);
        ReflectionTestUtils.setField(faceService, "encryptKey", "test-key-for-crypto-16b");
        ReflectionTestUtils.setField(faceService, "storeOriginalImage", false);
    }

    // ── 反射辅助：加解密往返测试 ──

    private byte[] callEncrypt(byte[] data) throws Exception {
        Method m = FaceServiceImpl.class.getDeclaredMethod("encrypt", byte[].class);
        m.setAccessible(true);
        return (byte[]) m.invoke(faceService, (Object) data);
    }

    private byte[] callDecrypt(byte[] data) throws Exception {
        Method m = FaceServiceImpl.class.getDeclaredMethod("decrypt", byte[].class);
        m.setAccessible(true);
        return (byte[]) m.invoke(faceService, (Object) data);
    }

    // ── 加解密 ──

    @Nested
    @DisplayName("特征加解密")
    class Crypto {

        @Test
        @DisplayName("加密后解密还原原始数据")
        void encryptDecrypt_roundTrip() throws Exception {
            byte[] original = "test-feature-data-1234567890".getBytes();
            byte[] encrypted = callEncrypt(original);
            byte[] decrypted = callDecrypt(encrypted);

            assertNotNull(encrypted);
            assertNotEquals(0, encrypted.length);
            assertArrayEquals(original, decrypted);
        }

        @Test
        @DisplayName("512字节特征数据往返一致")
        void largeFeature_roundTrip() throws Exception {
            byte[] original = new byte[512];
            for (int i = 0; i < 512; i++) original[i] = (byte) (i % 256);

            byte[] encrypted = callEncrypt(original);
            byte[] decrypted = callDecrypt(encrypted);

            assertArrayEquals(original, decrypted);
        }

        @Test
        @DisplayName("加密结果不等于原文")
        void encrypt_producesDifferentBytes() throws Exception {
            byte[] original = "hello-feature".getBytes();
            byte[] encrypted = callEncrypt(original);

            // AES 加密后不应该是明文
            boolean same = original.length == encrypted.length;
            if (same) {
                for (int i = 0; i < original.length; i++) {
                    if (original[i] != encrypted[i]) { same = false; break; }
                }
            }
            assertFalse(same, "加密结果不应与明文相同");
        }

        @Test
        @DisplayName("解密非法数据抛出异常")
        void decrypt_invalidData_throws() {
            assertThrows(Exception.class, () -> callDecrypt(new byte[]{1, 2, 3}));
        }
    }

    // ── 录入 ──

    @Nested
    @DisplayName("人脸录入")
    class Enroll {

        private FaceExtractResponse validResponse() {
            FaceExtractResponse.ExtractData data = new FaceExtractResponse.ExtractData();
            data.setFaceFound(true);
            data.setFaceCount(1);
            data.setQuality(0.85f);
            data.setFeature(Base64.getEncoder().encodeToString("test-feature-bytes-16b".getBytes()));
            data.setModelVer("arcsoft-v3.0");
            FaceExtractResponse resp = new FaceExtractResponse();
            resp.setCode(0);
            resp.setData(data);
            return resp;
        }

        @Test
        @DisplayName("录入成功")
        void enroll_success() {
            when(alumniMapper.selectById(1L)).thenReturn(new Alumni());
            when(restTemplate.postForObject(eq("http://localhost:8000/extract"), any(), eq(FaceExtractResponse.class)))
                    .thenReturn(validResponse());
            when(faceFeatureMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            MockMultipartFile file = new MockMultipartFile("file", "face.jpg", "image/jpeg",
                    "fake-image-bytes".getBytes());
            FaceEnrollVO result = faceService.enroll(1L, file);

            assertEquals(1L, result.getAlumniId());
            assertEquals(0.85f, result.getQuality());
            assertEquals(1, result.getFaceStatus());
            verify(alumniMapper).updateById(argThat(a -> a.getFaceStatus() == 1));
        }

        @Test
        @DisplayName("校友不存在抛出异常")
        void alumniNotFound_throws() {
            when(alumniMapper.selectById(999L)).thenReturn(null);

            MockMultipartFile file = new MockMultipartFile("file", "face.jpg", "image/jpeg",
                    "fake".getBytes());
            assertThrows(BizException.class, () -> faceService.enroll(999L, file));
        }

        @Test
        @DisplayName("未检测到人脸抛出 5101")
        void noFaceDetected_throws() {
            when(alumniMapper.selectById(1L)).thenReturn(new Alumni());
            FaceExtractResponse resp = new FaceExtractResponse();
            FaceExtractResponse.ExtractData data = new FaceExtractResponse.ExtractData();
            data.setFaceFound(false);
            resp.setCode(0);
            resp.setData(data);
            when(restTemplate.postForObject(anyString(), any(), eq(FaceExtractResponse.class)))
                    .thenReturn(resp);

            MockMultipartFile file = new MockMultipartFile("file", "face.jpg", "image/jpeg",
                    "fake".getBytes());
            BizException ex = assertThrows(BizException.class, () -> faceService.enroll(1L, file));
            assertEquals(5101, ex.getCode());
        }

        @Test
        @DisplayName("检测到多张人脸抛出 5103")
        void multipleFaces_throws() {
            when(alumniMapper.selectById(1L)).thenReturn(new Alumni());
            FaceExtractResponse resp = new FaceExtractResponse();
            FaceExtractResponse.ExtractData data = new FaceExtractResponse.ExtractData();
            data.setFaceFound(true);
            data.setFaceCount(3);  // multiple faces
            resp.setCode(0);
            resp.setData(data);
            when(restTemplate.postForObject(anyString(), any(), eq(FaceExtractResponse.class)))
                    .thenReturn(resp);

            MockMultipartFile file = new MockMultipartFile("file", "face.jpg", "image/jpeg",
                    "fake".getBytes());
            BizException ex = assertThrows(BizException.class, () -> faceService.enroll(1L, file));
            assertEquals(5103, ex.getCode());
        }

        @Test
        @DisplayName("人脸质量不达标抛出 5102")
        void lowQuality_throws() {
            when(alumniMapper.selectById(1L)).thenReturn(new Alumni());
            FaceExtractResponse resp = new FaceExtractResponse();
            FaceExtractResponse.ExtractData data = new FaceExtractResponse.ExtractData();
            data.setFaceFound(true);
            data.setFaceCount(1);
            data.setQuality(0.3f);  // below 0.6 threshold
            resp.setCode(0);
            resp.setData(data);
            when(restTemplate.postForObject(anyString(), any(), eq(FaceExtractResponse.class)))
                    .thenReturn(resp);

            MockMultipartFile file = new MockMultipartFile("file", "face.jpg", "image/jpeg",
                    "fake".getBytes());
            BizException ex = assertThrows(BizException.class, () -> faceService.enroll(1L, file));
            assertEquals(5102, ex.getCode());
        }

        @Test
        @DisplayName("face-service 不可用抛出 5104")
        void faceServiceUnavailable_throws() {
            when(alumniMapper.selectById(1L)).thenReturn(new Alumni());
            when(restTemplate.postForObject(anyString(), any(), eq(FaceExtractResponse.class)))
                    .thenThrow(new RestClientException("Connection refused"));

            MockMultipartFile file = new MockMultipartFile("file", "face.jpg", "image/jpeg",
                    "fake".getBytes());
            BizException ex = assertThrows(BizException.class, () -> faceService.enroll(1L, file));
            assertEquals(5104, ex.getCode());
        }
    }

    // ── 识别 ──

    @Nested
    @DisplayName("人脸识别")
    class Recognize {

        private static final byte[] PLAIN_FEATURE = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

        private FaceRecognizeRequest request() {
            FaceRecognizeRequest req = new FaceRecognizeRequest();
            req.setImage("base64-image-data");
            req.setDevice("hall-01");
            return req;
        }

        @Test
        @DisplayName("底库为空返回 NO_MATCH")
        void emptyGallery_returnsNoMatch() {
            when(faceFeatureMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of());

            FaceRecognizeVO result = faceService.recognize(request());

            assertEquals("NO_MATCH", result.getStatus());
            assertNull(result.getAlumniId());
        }

        @Test
        @DisplayName("特征解密失败降级为 DEGRADED")
        void decryptFailure_degraded() {
            // 用未加密的原始字节 → 解密失败
            FaceFeature ff = createFeature(1L, PLAIN_FEATURE);
            when(faceFeatureMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(ff));

            FaceRecognizeVO result = faceService.recognize(request());

            assertEquals("DEGRADED", result.getStatus());
        }

        @Test
        @DisplayName("无匹配返回 NO_MATCH")
        void noMatch_returnsNoMatch() throws Exception {
            FaceFeature ff = createEncryptedFeature(1L, PLAIN_FEATURE);
            when(faceFeatureMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(ff));

            FaceMatchResponse matchResp = new FaceMatchResponse();
            FaceMatchResponse.MatchData data = new FaceMatchResponse.MatchData();
            data.setHit(false);
            data.setFaceFound(true);
            matchResp.setCode(0);
            matchResp.setData(data);
            when(restTemplate.postForObject(eq("http://localhost:8000/match"), any(), eq(FaceMatchResponse.class)))
                    .thenReturn(matchResp);

            FaceRecognizeVO result = faceService.recognize(request());

            assertEquals("NO_MATCH", result.getStatus());
        }

        @Test
        @DisplayName("face-service 调用失败降级为 DEGRADED")
        void matchFailure_degraded() throws Exception {
            FaceFeature ff = createEncryptedFeature(1L, PLAIN_FEATURE);
            when(faceFeatureMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(ff));
            when(restTemplate.postForObject(eq("http://localhost:8000/match"), any(), eq(FaceMatchResponse.class)))
                    .thenThrow(new RestClientException("Timeout"));

            FaceRecognizeVO result = faceService.recognize(request());

            assertEquals("DEGRADED", result.getStatus());
        }

        @Test
        @DisplayName("match 返回 null 降级为 DEGRADED")
        void matchNullResponse_degraded() throws Exception {
            FaceFeature ff = createEncryptedFeature(1L, PLAIN_FEATURE);
            when(faceFeatureMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(ff));
            when(restTemplate.postForObject(anyString(), any(), eq(FaceMatchResponse.class)))
                    .thenReturn(null);

            FaceRecognizeVO result = faceService.recognize(request());

            assertEquals("DEGRADED", result.getStatus());
        }

        @Test
        @DisplayName("match 返回非 0 code 降级")
        void matchErrorCode_degraded() throws Exception {
            FaceFeature ff = createEncryptedFeature(1L, PLAIN_FEATURE);
            when(faceFeatureMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(ff));
            FaceMatchResponse resp = new FaceMatchResponse();
            resp.setCode(-1);
            when(restTemplate.postForObject(anyString(), any(), eq(FaceMatchResponse.class)))
                    .thenReturn(resp);

            FaceRecognizeVO result = faceService.recognize(request());

            assertEquals("DEGRADED", result.getStatus());
        }

        @Test
        @DisplayName("命中返回 HIT 及校友信息和时间线")
        void hit_returnsAlumniAndTimeline() throws Exception {
            FaceFeature ff = createEncryptedFeature(1L, PLAIN_FEATURE);
            when(faceFeatureMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(ff));

            FaceMatchResponse matchResp = new FaceMatchResponse();
            FaceMatchResponse.MatchData data = new FaceMatchResponse.MatchData();
            data.setHit(true);
            data.setFaceFound(true);
            data.setAlumniId(100L);
            data.setScore(0.92f);
            matchResp.setCode(0);
            matchResp.setData(data);
            when(restTemplate.postForObject(anyString(), any(), eq(FaceMatchResponse.class)))
                    .thenReturn(matchResp);

            Alumni alumni = new Alumni();
            alumni.setId(100L);
            alumni.setName("张三");
            alumni.setCollegeId(1L);
            when(alumniMapper.selectById(100L)).thenReturn(alumni);
            SysCollege college = new SysCollege();
            college.setName("计算机学院");
            when(collegeMapper.selectById(1L)).thenReturn(college);
            when(archiveMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

            FaceRecognizeVO result = faceService.recognize(request());

            assertEquals("HIT", result.getStatus());
            assertEquals(100L, result.getAlumniId());
            assertEquals(0.92f, result.getScore());
            assertEquals("张三", result.getAlumni().getName());
        }

        @Test
        @DisplayName("命中但校友不存在返回 NO_MATCH")
        void hitButAlumniNotFound_returnsNoMatch() throws Exception {
            FaceFeature ff = createEncryptedFeature(1L, PLAIN_FEATURE);
            when(faceFeatureMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(ff));

            FaceMatchResponse matchResp = new FaceMatchResponse();
            FaceMatchResponse.MatchData data = new FaceMatchResponse.MatchData();
            data.setHit(true);
            data.setFaceFound(true);
            data.setAlumniId(999L);
            matchResp.setCode(0);
            matchResp.setData(data);
            when(restTemplate.postForObject(anyString(), any(), eq(FaceMatchResponse.class)))
                    .thenReturn(matchResp);
            when(alumniMapper.selectById(999L)).thenReturn(null);

            FaceRecognizeVO result = faceService.recognize(request());

            assertEquals("NO_MATCH", result.getStatus());
        }
    }

    private FaceFeature createFeature(Long alumniId, byte[] featureBytes) {
        FaceFeature ff = new FaceFeature();
        ff.setAlumniId(alumniId);
        ff.setFeature(featureBytes);
        ff.setStatus(1);
        return ff;
    }

    /** 创建已加密的 Feature（用于需要经过解密步骤到达 HTTP 调用的测试） */
    private FaceFeature createEncryptedFeature(Long alumniId, byte[] plainBytes) throws Exception {
        byte[] encrypted = callEncrypt(plainBytes);
        return createFeature(alumniId, encrypted);
    }
}
