package com.campus.exhibition.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exhibition.common.R;
import com.campus.exhibition.dto.CollegeQuery;
import com.campus.exhibition.dto.CollegeSaveRequest;
import com.campus.exhibition.service.SysCollegeService;
import com.campus.exhibition.vo.CollegeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学院字典管理
 */
@RestController
@RequestMapping("/api/college")
@RequiredArgsConstructor
public class SysCollegeController {

    private final SysCollegeService collegeService;

    @GetMapping
    @PreAuthorize("hasAuthority('admin')")
    public R<Page<CollegeVO>> page(
            CollegeQuery query,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(collegeService.page(query, pageNum, pageSize));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('admin') or hasAuthority('academic') or hasAuthority('college')")
    public R<List<CollegeVO>> listAll() {
        return R.ok(collegeService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public R<CollegeVO> getById(@PathVariable Long id) {
        return R.ok(collegeService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('admin')")
    public R<CollegeVO> create(@Valid @RequestBody CollegeSaveRequest request) {
        return R.ok(collegeService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public R<CollegeVO> update(@PathVariable Long id, @Valid @RequestBody CollegeSaveRequest request) {
        return R.ok(collegeService.update(id, request));
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('admin')")
    public R<Void> toggleStatus(@PathVariable Long id) {
        collegeService.toggleStatus(id);
        return R.ok();
    }
}
