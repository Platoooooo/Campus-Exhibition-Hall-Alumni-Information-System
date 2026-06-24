package com.campus.exhibition.enums;

import lombok.Getter;

/**
 * 资料档案状态机
 * <pre>
 * draft → pending_college → pending_academic → approved → published ⇄ unpublished
 * 任意审核环节可 → rejected
 * </pre>
 */
@Getter
public enum ArchiveStatus {

    DRAFT("draft", "草稿"),
    PENDING_COLLEGE("pending_college", "待学院审核"),
    PENDING_ACADEMIC("pending_academic", "待教务处审核"),
    APPROVED("approved", "已入库"),
    REJECTED("rejected", "已驳回"),
    PUBLISHED("published", "已上架"),
    UNPUBLISHED("unpublished", "已下架");

    private final String code;
    private final String label;

    ArchiveStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static ArchiveStatus fromCode(String code) {
        for (ArchiveStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }

    /**
     * 是否允许从当前状态流转到目标状态
     */
    public boolean canTransitionTo(ArchiveStatus target) {
        return switch (this) {
            case DRAFT            -> target == PENDING_COLLEGE;
            case PENDING_COLLEGE  -> target == PENDING_ACADEMIC || target == REJECTED;
            case PENDING_ACADEMIC -> target == APPROVED || target == REJECTED;
            case APPROVED         -> target == PUBLISHED;
            case REJECTED         -> target == DRAFT;          // 退回修改
            case PUBLISHED        -> target == UNPUBLISHED;
            case UNPUBLISHED      -> target == PUBLISHED;
        };
    }
}
