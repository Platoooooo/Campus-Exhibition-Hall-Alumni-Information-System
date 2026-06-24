package com.campus.exhibition.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exhibition.common.R;
import com.campus.exhibition.dto.MajorQuery;
import com.campus.exhibition.dto.MajorSaveRequest;
import com.campus.exhibition.service.SysMajorService;
import com.campus.exhibition.vo.MajorVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 专业字典管理
 */
@RestController
@RequestMapping("/api/major")
@RequiredArgsConstructor
public class SysMajorController {

    private final SysMajorService majorService;

    @GetMapping
    @PreAuthorize("hasAuthority('admin')")
    public R<Page<MajorVO>> page(
            MajorQuery query,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(majorService.page(query, pageNum, pageSize));
    }

    /** 按学院级联 */
    @GetMapping("/by-college")
    @PreAuthorize("hasAuthority('admin') or hasAuthority('academic') or hasAuthority('college')")
    public R<List<MajorVO>> byCollege(@RequestParam(required = false) Long collegeId) {
        return R.ok(majorService.listByCollege(collegeId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public R<MajorVO> getById(@PathVariable Long id) {
        return R.ok(majorService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('admin')")
    public R<MajorVO> create(@Valid @RequestBody MajorSaveRequest request) {
        return R.ok(majorService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public R<MajorVO> update(@PathVariable Long id, @Valid @RequestBody MajorSaveRequest request) {
        return R.ok(majorService.update(id, request));
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('admin')")
    public R<Void> toggleStatus(@PathVariable Long id) {
        majorService.toggleStatus(id);
        return R.ok();
    }
}
