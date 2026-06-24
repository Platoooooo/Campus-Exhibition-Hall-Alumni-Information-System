package com.campus.exhibition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.exhibition.common.DataScopeHelper;
import com.campus.exhibition.entity.Alumni;
import com.campus.exhibition.entity.Archive;
import com.campus.exhibition.entity.ArchiveCategory;
import com.campus.exhibition.entity.SysCollege;
import com.campus.exhibition.mapper.AlumniMapper;
import com.campus.exhibition.mapper.ArchiveCategoryMapper;
import com.campus.exhibition.mapper.ArchiveMapper;
import com.campus.exhibition.mapper.SysCollegeMapper;
import com.campus.exhibition.service.DashboardService;
import com.campus.exhibition.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AlumniMapper alumniMapper;
    private final ArchiveMapper archiveMapper;
    private final ArchiveCategoryMapper categoryMapper;
    private final SysCollegeMapper collegeMapper;

    @Override
    public DashboardVO stats() {
        Long collegeId = DataScopeHelper.visibleCollegeId();
        String role = com.campus.exhibition.common.UserContext.role();

        // ---- 统计卡片 ----
        long totalAlumni = countAlumni(collegeId);
        long pendingReview = countPendingReview(collegeId, role);
        long published = countPublished(collegeId);
        long newThisMonth = countNewAlumniThisMonth(collegeId);

        // ---- 分类占比 ----
        List<DashboardVO.CategoryPieItem> categoryDistribution = buildCategoryPie(collegeId);

        // ---- 审核流转（近12个月） ----
        List<DashboardVO.AuditFlowItem> auditFlow = buildAuditFlow(collegeId);

        // ---- 待办列表（top 10） ----
        List<DashboardVO.TodoItem> todoList = buildTodoList(collegeId, role);

        return DashboardVO.builder()
                .totalAlumni(totalAlumni)
                .pendingReview(pendingReview)
                .published(published)
                .newThisMonth(newThisMonth)
                .categoryDistribution(categoryDistribution)
                .auditFlow(auditFlow)
                .todoList(todoList)
                .build();
    }

    /* ---- 统计卡片逻辑 ---- */

    private long countAlumni(Long collegeId) {
        LambdaQueryWrapper<Alumni> w = new LambdaQueryWrapper<>();
        w.eq(Alumni::getStatus, 1);
        if (collegeId != null) w.eq(Alumni::getCollegeId, collegeId);
        return alumniMapper.selectCount(w);
    }

    private long countPendingReview(Long collegeId, String role) {
        LambdaQueryWrapper<Archive> w = new LambdaQueryWrapper<>();
        if ("college".equals(role)) {
            w.eq(Archive::getStatus, "pending_college");
            if (collegeId != null) w.eq(Archive::getCollegeId, collegeId);
        } else if ("academic".equals(role)) {
            w.eq(Archive::getStatus, "pending_academic");
        } else {
            // admin 看全部待审
            w.in(Archive::getStatus, "pending_college", "pending_academic");
        }
        return archiveMapper.selectCount(w);
    }

    private long countPublished(Long collegeId) {
        LambdaQueryWrapper<Archive> w = new LambdaQueryWrapper<>();
        w.eq(Archive::getStatus, "published");
        if (collegeId != null) w.eq(Archive::getCollegeId, collegeId);
        return archiveMapper.selectCount(w);
    }

    private long countNewAlumniThisMonth(Long collegeId) {
        LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
        LambdaQueryWrapper<Alumni> w = new LambdaQueryWrapper<>();
        w.ge(Alumni::getCreateTime, startOfMonth);
        w.eq(Alumni::getStatus, 1);
        if (collegeId != null) w.eq(Alumni::getCollegeId, collegeId);
        return alumniMapper.selectCount(w);
    }

    /* ---- 分类占比 ---- */

    private List<DashboardVO.CategoryPieItem> buildCategoryPie(Long collegeId) {
        // 查所有启用的分类
        List<ArchiveCategory> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<ArchiveCategory>().eq(ArchiveCategory::getStatus, 1));
        if (categories.isEmpty()) return List.of();

        // 按分类统计已发布的档案数
        Map<Long, String> idName = categories.stream()
                .collect(Collectors.toMap(ArchiveCategory::getId, ArchiveCategory::getName));

        LambdaQueryWrapper<Archive> w = new LambdaQueryWrapper<>();
        w.eq(Archive::getStatus, "published");
        w.in(Archive::getCategoryId, idName.keySet());
        if (collegeId != null) w.eq(Archive::getCollegeId, collegeId);

        List<Archive> archives = archiveMapper.selectList(w);
        Map<Long, Long> catCount = archives.stream()
                .collect(Collectors.groupingBy(Archive::getCategoryId, Collectors.counting()));

        // 确保所有分类都出现（值可能为 0）
        return idName.entrySet().stream()
                .map(e -> DashboardVO.CategoryPieItem.builder()
                        .name(e.getValue())
                        .value(catCount.getOrDefault(e.getKey(), 0L))
                        .build())
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());
    }

    /* ---- 审核流转（近12个月按月聚合） ---- */

    private List<DashboardVO.AuditFlowItem> buildAuditFlow(Long collegeId) {
        List<DashboardVO.AuditFlowItem> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");

        // 生成近 12 个月标签
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            String label = ym.format(fmt);

            // 当月起始
            LocalDateTime start = ym.atDay(1).atStartOfDay();
            LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();

            LambdaQueryWrapper<Archive> w = new LambdaQueryWrapper<>();
            w.ge(Archive::getCreateTime, start);
            w.lt(Archive::getCreateTime, end);
            if (collegeId != null) w.eq(Archive::getCollegeId, collegeId);

            List<Archive> monthArchives = archiveMapper.selectList(w);
            long pendingCollege = monthArchives.stream()
                    .filter(a -> "pending_college".equals(a.getStatus())).count();
            long pendingAcademic = monthArchives.stream()
                    .filter(a -> "pending_academic".equals(a.getStatus())).count();
            long approved = monthArchives.stream()
                    .filter(a -> "approved".equals(a.getStatus())
                            || "published".equals(a.getStatus())).count();
            long rejected = monthArchives.stream()
                    .filter(a -> "rejected".equals(a.getStatus())).count();

            result.add(DashboardVO.AuditFlowItem.builder()
                    .month(label)
                    .pendingCollege(pendingCollege)
                    .pendingAcademic(pendingAcademic)
                    .approved(approved)
                    .rejected(rejected)
                    .build());
        }
        return result;
    }

    /* ---- 待办列表 ---- */

    private List<DashboardVO.TodoItem> buildTodoList(Long collegeId, String role) {
        LambdaQueryWrapper<Archive> w = new LambdaQueryWrapper<>();
        if ("college".equals(role)) {
            w.eq(Archive::getStatus, "pending_college");
            if (collegeId != null) w.eq(Archive::getCollegeId, collegeId);
        } else if ("academic".equals(role)) {
            w.eq(Archive::getStatus, "pending_academic");
        } else {
            w.in(Archive::getStatus, "pending_college", "pending_academic");
        }
        w.orderByDesc(Archive::getSubmitTime);
        w.last("LIMIT 10");

        List<Archive> archives = archiveMapper.selectList(w);
        if (archives.isEmpty()) return List.of();

        // 批量取分类名
        Set<Long> catIds = archives.stream().map(Archive::getCategoryId).collect(Collectors.toSet());
        Map<Long, String> catMap = categoryMapper.selectBatchIds(catIds).stream()
                .collect(Collectors.toMap(ArchiveCategory::getId, ArchiveCategory::getName));

        // 批量取学院名
        Set<Long> colIds = archives.stream().map(Archive::getCollegeId).collect(Collectors.toSet());
        Map<Long, String> colMap = colIds.isEmpty() ? Map.of() :
                collegeMapper.selectBatchIds(colIds).stream()
                        .collect(Collectors.toMap(SysCollege::getId, SysCollege::getName));

        return archives.stream().map(a -> DashboardVO.TodoItem.builder()
                .archiveId(a.getId())
                .title(a.getTitle())
                .categoryName(catMap.getOrDefault(a.getCategoryId(), ""))
                .collegeName(colMap.getOrDefault(a.getCollegeId(), ""))
                .status(a.getStatus())
                .submitTime(a.getSubmitTime() != null ? a.getSubmitTime().toString() : "")
                .build()
        ).collect(Collectors.toList());
    }
}
