package com.campus.exhibition.service;

import com.campus.exhibition.common.BizException;
import com.campus.exhibition.enums.MediaType;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * 文件存储服务 —— 上传、缩略图、白名单校验、大小校验
 */
@Slf4j
@Service
public class FileStorageService {

    @Value("${app.upload.base-dir:./uploads}")
    private String baseDir;

    @Value("${app.upload.max-image-size:10485760}")   // 10MB
    private long maxImageSize;

    @Value("${app.upload.max-video-size:104857600}")   // 100MB
    private long maxVideoSize;

    @Value("${app.upload.max-doc-size:20971520}")      // 20MB
    private long maxDocSize;

    @Getter
    private Path imageDir;
    @Getter
    private Path videoDir;
    @Getter
    private Path docDir;
    @Getter
    private Path thumbDir;

    private static final Set<String> IMAGE_EXTS = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp");
    private static final Set<String> VIDEO_EXTS = Set.of("mp4", "webm", "avi", "mov");
    private static final Set<String> DOC_EXTS   = Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx");

    @PostConstruct
    public void init() throws IOException {
        Path root = Path.of(baseDir).toAbsolutePath().normalize();
        imageDir = Files.createDirectories(root.resolve("images"));
        videoDir = Files.createDirectories(root.resolve("videos"));
        docDir   = Files.createDirectories(root.resolve("docs"));
        thumbDir = Files.createDirectories(root.resolve("thumbnails"));
        log.info("文件存储根目录: {}", root);
    }

    /** 上传文件，返回存储信息 */
    public UploadResult upload(MultipartFile file) {
        if (file.isEmpty()) throw new BizException(400, "文件为空");

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains("."))
            throw new BizException(400, "无法识别文件类型");

        String ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        MediaType mediaType = classify(ext);

        // 大小校验
        validateSize(file.getSize(), mediaType);

        // 生成存储文件名
        String storedName = UUID.randomUUID() + "." + ext;
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

        Path targetDir;
        switch (mediaType) {
            case IMAGE -> targetDir = imageDir.resolve(datePath);
            case VIDEO -> targetDir = videoDir.resolve(datePath);
            default    -> targetDir = docDir.resolve(datePath);
        }
        try {
            Files.createDirectories(targetDir);
            Path dest = targetDir.resolve(storedName);
            file.transferTo(dest);

            // 生成缩略图（图片）
            String thumbnailUrl = null;
            if (mediaType == MediaType.IMAGE) {
                thumbnailUrl = generateThumbnail(dest, datePath, storedName);
            }

            String url = "/uploads/" + mediaType.name().toLowerCase() + "s/" + datePath + "/" + storedName;

            Integer duration = null;
            // 视频时长：尝试从文件头读取（简单实现：置 null 表示未获取）
            // 实际项目可集成 FFprobe 或 JAVE2

            return new UploadResult(url, thumbnailUrl, mediaType.getCode(),
                    originalName, file.getSize(), duration);
        } catch (IOException e) {
            throw new BizException(400, "文件存储失败: " + e.getMessage());
        }
    }

    /** 生成缩略图 */
    private String generateThumbnail(Path imagePath, String datePath, String storedName) {
        try {
            BufferedImage original = ImageIO.read(imagePath.toFile());
            if (original == null) return null;

            int w = 400, h = 300;
            double ratio = Math.min((double) w / original.getWidth(), (double) h / original.getHeight());
            int newW = (int) (original.getWidth() * ratio);
            int newH = (int) (original.getHeight() * ratio);

            BufferedImage thumb = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = thumb.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(original, 0, 0, newW, newH, null);
            g.dispose();

            Path thumbTargetDir = thumbDir.resolve(datePath);
            Files.createDirectories(thumbTargetDir);
            Path thumbPath = thumbTargetDir.resolve("thumb_" + storedName);
            String thumbExt = storedName.substring(storedName.lastIndexOf('.') + 1);
            ImageIO.write(thumb, thumbExt.equals("jpg") || thumbExt.equals("jpeg") ? "jpg" : thumbExt, thumbPath.toFile());

            return "/uploads/thumbnails/" + datePath + "/thumb_" + storedName;
        } catch (Exception e) {
            log.warn("缩略图生成失败: {}", e.getMessage());
            return null;
        }
    }

    /** 根据扩展名分类 */
    private MediaType classify(String ext) {
        if (IMAGE_EXTS.contains(ext)) return MediaType.IMAGE;
        if (VIDEO_EXTS.contains(ext)) return MediaType.VIDEO;
        if (DOC_EXTS.contains(ext))   return MediaType.DOCUMENT;
        throw new BizException(400, "不支持的文件类型: ." + ext
                + "（图片: " + String.join("/", IMAGE_EXTS)
                + " 视频: " + String.join("/", VIDEO_EXTS)
                + " 文档: " + String.join("/", DOC_EXTS) + "）");
    }

    /** 文件大小白名单校验 */
    private void validateSize(long size, MediaType type) {
        long max = switch (type) {
            case IMAGE -> maxImageSize;
            case VIDEO -> maxVideoSize;
            case DOCUMENT -> maxDocSize;
        };
        if (size > max) {
            throw new BizException(400, "文件大小超出限制（最大 " + (max / 1024 / 1024) + "MB）");
        }
    }

    /** 删除文件 */
    public void delete(String url) {
        if (url == null) return;
        try {
            Path path = Path.of(baseDir).resolve(url.replaceFirst("^/uploads/", ""));
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("文件删除失败: {}", e.getMessage());
        }
    }

    /** 上传结果 */
    public record UploadResult(String url, String thumbnail, int type,
                               String fileName, long fileSize, Integer duration) {}
}
