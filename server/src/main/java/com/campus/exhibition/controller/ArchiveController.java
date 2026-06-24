package com.campus.exhibition.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exhibition.common.R;
import com.campus.exhibition.dto.ArchiveQuery;
import com.campus.exhibition.dto.ArchiveSaveRequest;
import com.campus.exhibition.service.ArchiveService;
import com.campus.exhibition.vo.ArchiveVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 资料档案管理
 */
@RestController
@RequestMapping("/api/archive")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;

    @GetMapping
    @PreAuthorize("hasAuthority('admin') or hasAuthority('academic') or hasAuthority('college')")
    public R<Page<ArchiveVO>> page(
            ArchiveQuery query,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(archiveService.page(query, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('admin') or hasAuthority('academic') or hasAuthority('college')")
    public R<ArchiveVO> getById(@PathVariable Long id) {
        return R.ok(archiveService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('admin') or hasAuthority('academic') or hasAuthority('college')")
    public R<ArchiveVO> create(@Valid @RequestBody ArchiveSaveRequest request) {
        return R.ok(archiveService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin') or hasAuthority('academic') or hasAuthority('college')")
    public R<ArchiveVO> update(@PathVariable Long id, @Valid @RequestBody ArchiveSaveRequest request) {
        return R.ok(archiveService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin') or hasAuthority('academic')")
    public R<Void> delete(@PathVariable Long id) {
        archiveService.delete(id);
        return R.ok();
    }

    /* ---- 媒体关联 ---- */

    @PostMapping("/{id}/media")
    @PreAuthorize("hasAuthority('admin') or hasAuthority('academic') or hasAuthority('college')")
    public R<ArchiveVO> addMedia(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return R.ok(archiveService.addMedia(id, file));
    }

    @DeleteMapping("/{archiveId}/media/{mediaId}")
    @PreAuthorize("hasAuthority('admin') or hasAuthority('academic') or hasAuthority('college')")
    public R<Void> removeMedia(@PathVariable Long archiveId, @PathVariable Long mediaId) {
        archiveService.removeMedia(archiveId, mediaId);
        return R.ok();
    }

    @PutMapping("/{archiveId}/media/sort")
    @PreAuthorize("hasAuthority('admin') or hasAuthority('academic') or hasAuthority('college')")
    public R<Void> sortMedia(@PathVariable Long archiveId, @RequestBody List<Long> mediaIds) {
        archiveService.sortMedia(archiveId, mediaIds);
        return R.ok();
    }
}
