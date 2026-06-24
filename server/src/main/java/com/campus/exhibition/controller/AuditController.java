package com.campus.exhibition.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exhibition.common.R;
import com.campus.exhibition.dto.AuditRequest;
import com.campus.exhibition.service.AuditService;
import com.campus.exhibition.vo.ArchiveVO;
import com.campus.exhibition.vo.AuditLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审核中心 —— 两级审核 + 待办 + 审核记录
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    /* ---- 提交 ---- */

    @PutMapping("/{archiveId}/submit")
    @PreAuthorize("hasAuthority('admin') or hasAuthority('academic') or hasAuthority('college')")
    public R<ArchiveVO> submit(@PathVariable Long archiveId) {
        return R.ok(auditService.submit(archiveId));
    }

    /* ---- 学院审核 ---- */

    @PutMapping("/{archiveId}/college/approve")
    @PreAuthorize("hasAuthority('admin') or hasAuthority('college')")
    public R<ArchiveVO> collegeApprove(@PathVariable Long archiveId,
                                       @RequestBody(required = false) AuditRequest request) {
        return R.ok(auditService.collegeApprove(archiveId,
                request != null ? request.getOpinion() : null));
    }

    @PutMapping("/{archiveId}/college/reject")
    @PreAuthorize("hasAuthority('admin') or hasAuthority('college')")
    public R<ArchiveVO> collegeReject(@PathVariable Long archiveId,
                                      @RequestBody AuditRequest request) {
        return R.ok(auditService.collegeReject(archiveId,
                request != null ? request.getOpinion() : null));
    }

    /* ---- 教务处审核 ---- */

    @PutMapping("/{archiveId}/academic/approve")
    @PreAuthorize("hasAuthority('admin') or hasAuthority('academic')")
    public R<ArchiveVO> academicApprove(@PathVariable Long archiveId,
                                        @RequestBody(required = false) AuditRequest request) {
        return R.ok(auditService.academicApprove(archiveId,
                request != null ? request.getOpinion() : null));
    }

    @PutMapping("/{archiveId}/academic/reject")
    @PreAuthorize("hasAuthority('admin') or hasAuthority('academic')")
    public R<ArchiveVO> academicReject(@PathVariable Long archiveId,
                                       @RequestBody AuditRequest request) {
        return R.ok(auditService.academicReject(archiveId,
                request != null ? request.getOpinion() : null));
    }

    /* ---- 待办 ---- */

    @GetMapping("/college/todo")
    @PreAuthorize("hasAuthority('admin') or hasAuthority('college')")
    public R<Page<ArchiveVO>> collegeTodo(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(auditService.collegeTodo(pageNum, pageSize));
    }

    @GetMapping("/academic/todo")
    @PreAuthorize("hasAuthority('admin') or hasAuthority('academic')")
    public R<Page<ArchiveVO>> academicTodo(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(auditService.academicTodo(pageNum, pageSize));
    }

    /* ---- 审核记录 ---- */

    @GetMapping("/{archiveId}/logs")
    @PreAuthorize("hasAuthority('admin') or hasAuthority('academic') or hasAuthority('college')")
    public R<List<AuditLogVO>> auditLogs(@PathVariable Long archiveId) {
        return R.ok(auditService.auditLogs(archiveId));
    }
}
