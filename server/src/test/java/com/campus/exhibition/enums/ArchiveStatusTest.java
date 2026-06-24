package com.campus.exhibition.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ArchiveStatus 状态机转换矩阵测试
 */
@DisplayName("ArchiveStatus 状态机")
class ArchiveStatusTest {

    // ── 合法转换路径（共 9 条） ──

    @ParameterizedTest(name = "{0} → {1} 合法")
    @CsvSource({
            "DRAFT,            PENDING_COLLEGE",
            "PENDING_COLLEGE,  PENDING_ACADEMIC",
            "PENDING_COLLEGE,  REJECTED",
            "PENDING_ACADEMIC, APPROVED",
            "PENDING_ACADEMIC, REJECTED",
            "APPROVED,         PUBLISHED",
            "REJECTED,         DRAFT",
            "PUBLISHED,        UNPUBLISHED",
            "UNPUBLISHED,      PUBLISHED",
    })
    void validTransitions(ArchiveStatus from, ArchiveStatus target) {
        assertTrue(from.canTransitionTo(target),
                () -> from.getLabel() + " 应该能转换到 " + target.getLabel());
    }

    // ── 非法转换路径（每状态至少 2 条） ──

    @Test
    @DisplayName("草稿只能转到待学院审核")
    void draft_onlyToPendingCollege() {
        assertFalse(ArchiveStatus.DRAFT.canTransitionTo(ArchiveStatus.DRAFT));
        assertFalse(ArchiveStatus.DRAFT.canTransitionTo(ArchiveStatus.PENDING_ACADEMIC));
        assertFalse(ArchiveStatus.DRAFT.canTransitionTo(ArchiveStatus.APPROVED));
        assertFalse(ArchiveStatus.DRAFT.canTransitionTo(ArchiveStatus.REJECTED));
        assertFalse(ArchiveStatus.DRAFT.canTransitionTo(ArchiveStatus.PUBLISHED));
        assertFalse(ArchiveStatus.DRAFT.canTransitionTo(ArchiveStatus.UNPUBLISHED));
    }

    @Test
    @DisplayName("待学院审核只能转到待教务处或已驳回")
    void pendingCollege_onlyToPendingAcademicOrRejected() {
        assertFalse(ArchiveStatus.PENDING_COLLEGE.canTransitionTo(ArchiveStatus.DRAFT));
        assertFalse(ArchiveStatus.PENDING_COLLEGE.canTransitionTo(ArchiveStatus.APPROVED));
        assertFalse(ArchiveStatus.PENDING_COLLEGE.canTransitionTo(ArchiveStatus.PUBLISHED));
        assertFalse(ArchiveStatus.PENDING_COLLEGE.canTransitionTo(ArchiveStatus.UNPUBLISHED));
    }

    @Test
    @DisplayName("待教务处审核只能转到已入库或已驳回")
    void pendingAcademic_onlyToApprovedOrRejected() {
        assertFalse(ArchiveStatus.PENDING_ACADEMIC.canTransitionTo(ArchiveStatus.DRAFT));
        assertFalse(ArchiveStatus.PENDING_ACADEMIC.canTransitionTo(ArchiveStatus.PENDING_COLLEGE));
        assertFalse(ArchiveStatus.PENDING_ACADEMIC.canTransitionTo(ArchiveStatus.PUBLISHED));
        assertFalse(ArchiveStatus.PENDING_ACADEMIC.canTransitionTo(ArchiveStatus.UNPUBLISHED));
    }

    @Test
    @DisplayName("已入库只能转到已上架")
    void approved_onlyToPublished() {
        assertFalse(ArchiveStatus.APPROVED.canTransitionTo(ArchiveStatus.DRAFT));
        assertFalse(ArchiveStatus.APPROVED.canTransitionTo(ArchiveStatus.PENDING_COLLEGE));
        assertFalse(ArchiveStatus.APPROVED.canTransitionTo(ArchiveStatus.PENDING_ACADEMIC));
        assertFalse(ArchiveStatus.APPROVED.canTransitionTo(ArchiveStatus.REJECTED));
        assertFalse(ArchiveStatus.APPROVED.canTransitionTo(ArchiveStatus.UNPUBLISHED));
    }

    @Test
    @DisplayName("已驳回只能转到草稿（退回修改）")
    void rejected_onlyToDraft() {
        assertFalse(ArchiveStatus.REJECTED.canTransitionTo(ArchiveStatus.PENDING_COLLEGE));
        assertFalse(ArchiveStatus.REJECTED.canTransitionTo(ArchiveStatus.PENDING_ACADEMIC));
        assertFalse(ArchiveStatus.REJECTED.canTransitionTo(ArchiveStatus.APPROVED));
        assertFalse(ArchiveStatus.REJECTED.canTransitionTo(ArchiveStatus.PUBLISHED));
        assertFalse(ArchiveStatus.REJECTED.canTransitionTo(ArchiveStatus.UNPUBLISHED));
    }

    @Test
    @DisplayName("已上架只能转到已下架")
    void published_onlyToUnpublished() {
        assertFalse(ArchiveStatus.PUBLISHED.canTransitionTo(ArchiveStatus.DRAFT));
        assertFalse(ArchiveStatus.PUBLISHED.canTransitionTo(ArchiveStatus.PENDING_COLLEGE));
        assertFalse(ArchiveStatus.PUBLISHED.canTransitionTo(ArchiveStatus.PENDING_ACADEMIC));
        assertFalse(ArchiveStatus.PUBLISHED.canTransitionTo(ArchiveStatus.APPROVED));
        assertFalse(ArchiveStatus.PUBLISHED.canTransitionTo(ArchiveStatus.REJECTED));
    }

    @Test
    @DisplayName("已下架只能转到已上架")
    void unpublished_onlyToPublished() {
        assertFalse(ArchiveStatus.UNPUBLISHED.canTransitionTo(ArchiveStatus.DRAFT));
        assertFalse(ArchiveStatus.UNPUBLISHED.canTransitionTo(ArchiveStatus.PENDING_COLLEGE));
        assertFalse(ArchiveStatus.UNPUBLISHED.canTransitionTo(ArchiveStatus.PENDING_ACADEMIC));
        assertFalse(ArchiveStatus.UNPUBLISHED.canTransitionTo(ArchiveStatus.APPROVED));
        assertFalse(ArchiveStatus.UNPUBLISHED.canTransitionTo(ArchiveStatus.REJECTED));
    }

    // ── fromCode ──

    @ParameterizedTest(name = "fromCode(\"{0}\") = {1}")
    @CsvSource({
            "draft,             DRAFT",
            "pending_college,   PENDING_COLLEGE",
            "pending_academic,  PENDING_ACADEMIC",
            "approved,          APPROVED",
            "rejected,          REJECTED",
            "published,         PUBLISHED",
            "unpublished,       UNPUBLISHED",
    })
    void fromCode_validCode_returnsCorrectEnum(String code, ArchiveStatus expected) {
        assertEquals(expected, ArchiveStatus.fromCode(code));
    }

    @Test
    @DisplayName("fromCode 未知编码返回 null")
    void fromCode_unknownCode_returnsNull() {
        assertNull(ArchiveStatus.fromCode("nonexistent"));
        assertNull(ArchiveStatus.fromCode(""));
        assertNull(ArchiveStatus.fromCode(null));
    }

    // ── 标签完整性 ──

    @Test
    @DisplayName("所有状态标签非空")
    void allLabelsNonBlank() {
        for (ArchiveStatus status : ArchiveStatus.values()) {
            assertNotNull(status.getLabel(), status.name() + " 标签不应为 null");
            assertFalse(status.getLabel().isBlank(), status.name() + " 标签不应为空");
        }
    }

    // ── 全部 7 种状态均存在 ──

    @Test
    @DisplayName("状态机包含全部 7 个状态")
    void allSevenStatesPresent() {
        assertEquals(7, ArchiveStatus.values().length);
    }
}
