package com.campus.exhibition.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 工作台聚合数据视图
 */
@Data
@Builder
public class DashboardVO {

    /** 统计卡片 */
    private long totalAlumni;
    private long pendingReview;
    private long published;
    private long newThisMonth;

    /** 分类占比饼图 */
    private List<CategoryPieItem> categoryDistribution;

    /** 审核流转走势（近12个月） */
    private List<AuditFlowItem> auditFlow;

    /** 当前用户的待办列表（top 10） */
    private List<TodoItem> todoList;

    /* ---- 内嵌 DTO ---- */

    @Data
    @Builder
    public static class CategoryPieItem {
        private String name;
        private long value;
    }

    @Data
    @Builder
    public static class AuditFlowItem {
        private String month;
        private long pendingCollege;
        private long pendingAcademic;
        private long approved;
        private long rejected;
    }

    @Data
    @Builder
    public static class TodoItem {
        private Long archiveId;
        private String title;
        private String categoryName;
        private String submitterName;
        private String collegeName;
        private String status;
        private String submitTime;
    }
}
