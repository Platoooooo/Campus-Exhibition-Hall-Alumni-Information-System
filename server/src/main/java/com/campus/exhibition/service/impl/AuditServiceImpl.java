package com.campus.exhibition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exhibition.common.BizException;
import com.campus.exhibition.common.ErrorCode;
import com.campus.exhibition.common.UserContext;
import com.campus.exhibition.entity.*;
import com.campus.exhibition.enums.ArchiveStatus;
import com.campus.exhibition.enums.RoleEnum;
import com.campus.exhibition.mapper.*;
import com.campus.exhibition.service.AuditService;
import com.campus.exhibition.vo.ArchiveVO;
import com.campus.exhibition.vo.AuditLogVO;
import com.campus.exhibition.vo.MediaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final ArchiveMapper archiveMapper;
    private final AuditLogMapper auditLogMapper;
    private final AlumniMapper alumniMapper;
    private final SysCollegeMapper collegeMapper;
    private final ArchiveCategoryMapper categoryMapper;
    private final ArchiveMediaMapper mediaMapper;

    /* ========== 提交 ========== */

    @Override
    @Transactional
    public ArchiveVO submit(Long archiveId) {
        Archive archive = getArchive(archiveId);
        validateTransition(archive, ArchiveStatus.PENDING_COLLEGE);
        archive.setStatus(ArchiveStatus.PENDING_COLLEGE.getCode());
        archive.setSubmitUser(UserContext.userId());
        archive.setSubmitTime(LocalDateTime.now());
        archiveMapper.updateById(archive);
        return toVO(archive);
    }

    /* ========== 学院审核 ========== */

    @Override
    @Transactional
    public ArchiveVO collegeApprove(Long archiveId, String opinion) {
        checkRole(RoleEnum.COLLEGE);
        Archive archive = getArchive(archiveId);
        checkCollegeScope(archive);
        validateTransition(archive, ArchiveStatus.PENDING_ACADEMIC);

        archive.setStatus(ArchiveStatus.PENDING_ACADEMIC.getCode());
        archiveMapper.updateById(archive);
        writeAuditLog(archiveId, "college", "approve", opinion);
        return toVO(archive);
    }

    @Override
    @Transactional
    public ArchiveVO collegeReject(Long archiveId, String opinion) {
        checkRole(RoleEnum.COLLEGE);
        Archive archive = getArchive(archiveId);
        checkCollegeScope(archive);
        validateTransition(archive, ArchiveStatus.REJECTED);
        requireOpinion(opinion);

        archive.setStatus(ArchiveStatus.REJECTED.getCode());
        archiveMapper.updateById(archive);
        writeAuditLog(archiveId, "college", "reject", opinion);
        return toVO(archive);
    }

    /* ========== 教务处审核 ========== */

    @Override
    @Transactional
    public ArchiveVO academicApprove(Long archiveId, String opinion) {
        checkRole(RoleEnum.ACADEMIC);
        Archive archive = getArchive(archiveId);
        validateTransition(archive, ArchiveStatus.APPROVED);

        archive.setStatus(ArchiveStatus.APPROVED.getCode());
        archiveMapper.updateById(archive);
        writeAuditLog(archiveId, "academic", "approve", opinion);
        return toVO(archive);
    }

    @Override
    @Transactional
    public ArchiveVO academicReject(Long archiveId, String opinion) {
        checkRole(RoleEnum.ACADEMIC);
        Archive archive = getArchive(archiveId);
        validateTransition(archive, ArchiveStatus.REJECTED);
        requireOpinion(opinion);

        archive.setStatus(ArchiveStatus.REJECTED.getCode());
        archiveMapper.updateById(archive);
        writeAuditLog(archiveId, "academic", "reject", opinion);
        return toVO(archive);
    }

    /* ========== 待办列表 ========== */

    @Override
    public Page<ArchiveVO> collegeTodo(int pageNum, int pageSize) {
        checkRole(RoleEnum.COLLEGE);

        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<Archive>()
                .eq(Archive::getStatus, ArchiveStatus.PENDING_COLLEGE.getCode())
                .orderByAsc(Archive::getSubmitTime);

        // admin 看全校，college 看本院
        if (!RoleEnum.ADMIN.getCode().equals(UserContext.role())) {
            Long collegeId = UserContext.collegeId();
            if (collegeId == null) throw new BizException(400, "学院管理员必须绑定学院");
            wrapper.eq(Archive::getCollegeId, collegeId);
        }

        Page<Archive> page = archiveMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
        return toVOPage(page, pageNum, pageSize);
    }

    @Override
    public Page<ArchiveVO> academicTodo(int pageNum, int pageSize) {
        checkRole(RoleEnum.ACADEMIC);

        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<Archive>()
                .eq(Archive::getStatus, ArchiveStatus.PENDING_ACADEMIC.getCode())
                .orderByAsc(Archive::getSubmitTime);

        Page<Archive> page = archiveMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
        return toVOPage(page, pageNum, pageSize);
    }

    /* ========== 审核记录 ========== */

    @Override
    public List<AuditLogVO> auditLogs(Long archiveId) {
        List<AuditLog> logs = auditLogMapper.selectList(
                new LambdaQueryWrapper<AuditLog>()
                        .eq(AuditLog::getArchiveId, archiveId)
                        .orderByAsc(AuditLog::getCreateTime));
        return logs.stream().map(log -> {
            AuditLogVO vo = new AuditLogVO();
            BeanUtils.copyProperties(log, vo);
            Archive archive = archiveMapper.selectById(archiveId);
            if (archive != null) vo.setArchiveTitle(archive.getTitle());
            return vo;
        }).collect(Collectors.toList());
    }

    /* ========== 内部校验 ========== */

    private Archive getArchive(Long id) {
        Archive archive = archiveMapper.selectById(id);
        if (archive == null) throw new BizException(ErrorCode.ARCHIVE_NOT_FOUND);
        return archive;
    }

    private void validateTransition(Archive archive, ArchiveStatus target) {
        ArchiveStatus current = ArchiveStatus.fromCode(archive.getStatus());
        if (current == null) throw new BizException(ErrorCode.AUDIT_STATUS_INVALID, "未知状态: " + archive.getStatus());
        if (!current.canTransitionTo(target)) {
            throw new BizException(ErrorCode.AUDIT_STATUS_INVALID,
                    "不允许从 " + current.getLabel() + " 流转到 " + target.getLabel());
        }
    }

    private void checkRole(RoleEnum requiredRole) {
        String role = UserContext.role();
        if (role == null) throw new BizException(ErrorCode.FORBIDDEN, "未登录");
        // admin 可以操作所有审核环节
        if (RoleEnum.ADMIN.getCode().equals(role)) return;
        if (!requiredRole.getCode().equals(role)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅" + requiredRole.getLabel() + "或校级管理员可操作");
        }
    }

    private void checkCollegeScope(Archive archive) {
        // 校级管理员不受学院限制
        if (RoleEnum.ADMIN.getCode().equals(UserContext.role())) return;
        Long userCollegeId = UserContext.collegeId();
        if (userCollegeId == null || !userCollegeId.equals(archive.getCollegeId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只能审核本学院资料");
        }
    }

    private void requireOpinion(String opinion) {
        if (opinion == null || opinion.isBlank()) {
            throw new BizException(ErrorCode.AUDIT_OPINION_REQUIRED);
        }
    }

    private void writeAuditLog(Long archiveId, String node, String action, String opinion) {
        AuditLog log = new AuditLog();
        log.setArchiveId(archiveId);
        log.setNode(node);
        log.setAction(action);
        log.setOpinion(opinion);
        log.setAuditorId(UserContext.userId());
        log.setAuditorName(UserContext.currentUser() != null
                ? UserContext.currentUser().getUsername() : null);
        auditLogMapper.insert(log);
    }

    /* ========== VO 转换 ========== */

    private ArchiveVO toVO(Archive entity) {
        ArchiveVO vo = new ArchiveVO();
        BeanUtils.copyProperties(entity, vo);
        Alumni alumni = alumniMapper.selectById(entity.getAlumniId());
        if (alumni != null) vo.setAlumniName(alumni.getName());
        SysCollege college = collegeMapper.selectById(entity.getCollegeId());
        if (college != null) vo.setCollegeName(college.getName());
        ArchiveCategory cat = categoryMapper.selectById(entity.getCategoryId());
        if (cat != null) vo.setCategoryName(cat.getName());

        List<ArchiveMedia> mediaList = mediaMapper.selectList(
                new LambdaQueryWrapper<ArchiveMedia>()
                        .eq(ArchiveMedia::getArchiveId, entity.getId())
                        .orderByAsc(ArchiveMedia::getSort));
        vo.setMediaList(mediaList.stream().map(m -> {
            MediaVO mv = new MediaVO();
            BeanUtils.copyProperties(m, mv);
            return mv;
        }).collect(Collectors.toList()));
        return vo;
    }

    private Page<ArchiveVO> toVOPage(Page<Archive> page, long pageNum, long pageSize) {
        Page<ArchiveVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }
}
