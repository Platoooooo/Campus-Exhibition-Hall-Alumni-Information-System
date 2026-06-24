package com.campus.exhibition.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exhibition.vo.ArchiveVO;
import com.campus.exhibition.vo.AuditLogVO;

import java.util.List;

public interface AuditService {

    /** 提交审核：draft → pending_college */
    ArchiveVO submit(Long archiveId);

    /** 学院通过：pending_college → pending_academic */
    ArchiveVO collegeApprove(Long archiveId, String opinion);

    /** 学院驳回：pending_college → rejected（意见必填） */
    ArchiveVO collegeReject(Long archiveId, String opinion);

    /** 教务处通过：pending_academic → approved */
    ArchiveVO academicApprove(Long archiveId, String opinion);

    /** 教务处驳回：pending_academic → rejected（意见必填） */
    ArchiveVO academicReject(Long archiveId, String opinion);

    /** 学院待办列表（本学院 pending_college） */
    Page<ArchiveVO> collegeTodo(int pageNum, int pageSize);

    /** 教务处待办列表（pending_academic） */
    Page<ArchiveVO> academicTodo(int pageNum, int pageSize);

    /** 查看某档案的审核记录 */
    List<AuditLogVO> auditLogs(Long archiveId);
}
