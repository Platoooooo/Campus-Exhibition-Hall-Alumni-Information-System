package com.campus.exhibition.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.exhibition.common.BizException;
import com.campus.exhibition.common.R;
import com.campus.exhibition.entity.Archive;
import com.campus.exhibition.entity.ArchiveMedia;
import com.campus.exhibition.enums.ArchiveStatus;
import com.campus.exhibition.mapper.ArchiveMapper;
import com.campus.exhibition.mapper.ArchiveMediaMapper;
import com.campus.exhibition.service.FileStorageService;
import com.campus.exhibition.vo.MediaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 媒体资源：上传 + 公开访问
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MediaController {

    private final FileStorageService fileStorage;
    private final ArchiveMediaMapper mediaMapper;
    private final ArchiveMapper archiveMapper;

    /** 上传媒体文件（独立上传，后续绑定到档案） */
    @PostMapping("/media/upload")
    @PreAuthorize("hasAuthority('admin') or hasAuthority('academic') or hasAuthority('college')")
    public R<FileStorageService.UploadResult> upload(@RequestParam("file") MultipartFile file) {
        return R.ok(fileStorage.upload(file));
    }

    /** 获取媒体详情（需认证） */
    @GetMapping("/media/{id}")
    @PreAuthorize("hasAuthority('admin') or hasAuthority('academic') or hasAuthority('college')")
    public R<MediaVO> getById(@PathVariable Long id) {
        ArchiveMedia entity = mediaMapper.selectById(id);
        if (entity == null) throw new BizException(404, "媒体不存在");
        MediaVO vo = new MediaVO();
        BeanUtils.copyProperties(entity, vo);
        return R.ok(vo);
    }

    /**
     * 公开访问媒体文件（仅限已上架档案的媒体）
     */
    @GetMapping("/public/media/{id}")
    public ResponseEntity<Resource> publicAccess(@PathVariable Long id) {
        ArchiveMedia media = mediaMapper.selectById(id);
        if (media == null) return ResponseEntity.notFound().build();

        // 校验所属档案是否已上架
        Archive archive = archiveMapper.selectById(media.getArchiveId());
        if (archive == null || !ArchiveStatus.PUBLISHED.getCode().equals(archive.getStatus())) {
            return ResponseEntity.status(403).build();
        }

        return serveFile(media.getUrl());
    }

    /** 公开访问缩略图 */
    @GetMapping("/public/thumb/{id}")
    public ResponseEntity<Resource> publicThumb(@PathVariable Long id) {
        ArchiveMedia media = mediaMapper.selectById(id);
        if (media == null || media.getThumbnail() == null) return ResponseEntity.notFound().build();

        Archive archive = archiveMapper.selectById(media.getArchiveId());
        if (archive == null || !ArchiveStatus.PUBLISHED.getCode().equals(archive.getStatus())) {
            return ResponseEntity.status(403).build();
        }

        return serveFile(media.getThumbnail());
    }

    private ResponseEntity<Resource> serveFile(String url) {
        Path path = Path.of(fileStorage.getImageDir().getParent().toString(),
                url.replaceFirst("^/uploads/", ""));
        if (!Files.exists(path)) return ResponseEntity.notFound().build();

        Resource resource = new FileSystemResource(path);
        String filename = path.getFileName().toString();
        String contentType = detectContentType(filename);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    private String detectContentType(String name) {
        name = name.toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".mp4")) return "video/mp4";
        if (name.endsWith(".webm")) return "video/webm";
        if (name.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }
}
