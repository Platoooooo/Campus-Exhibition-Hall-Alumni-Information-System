package com.campus.exhibition.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ErrorCode 错误码枚举测试
 */
@DisplayName("ErrorCode 错误码")
class ErrorCodeTest {

    @Test
    @DisplayName("所有错误码唯一")
    void allCodesUnique() {
        Set<Integer> seen = new HashSet<>();
        for (ErrorCode ec : ErrorCode.values()) {
            assertFalse(seen.contains(ec.getCode()),
                    () -> "错误码重复: " + ec.getCode());
            seen.add(ec.getCode());
        }
    }

    @Test
    @DisplayName("所有消息非空")
    void allMessagesNonBlank() {
        for (ErrorCode ec : ErrorCode.values()) {
            assertNotNull(ec.getMessage(), ec.name() + " 消息不应为 null");
            assertFalse(ec.getMessage().isBlank(), ec.name() + " 消息不应为空");
        }
    }

    @Test
    @DisplayName("所有错误码非负")
    void allCodesNonNegative() {
        for (ErrorCode ec : ErrorCode.values()) {
            assertTrue(ec.getCode() >= 0,
                    () -> ec.name() + " 错误码不应为负数: " + ec.getCode());
        }
    }

    @Test
    @DisplayName("SUCCESS 错误码为 200")
    void successCodeIs200() {
        assertEquals(200, ErrorCode.SUCCESS.getCode());
    }

    @Test
    @DisplayName("通用错误码范围 1xx-5xx")
    void commonCodeRange() {
        assertTrue(ErrorCode.BAD_REQUEST.getCode() >= 400 && ErrorCode.BAD_REQUEST.getCode() < 500);
        assertTrue(ErrorCode.INTERNAL_ERROR.getCode() >= 500);
    }

    @Test
    @DisplayName("审核错误码范围 3xxx")
    void auditCodeRange() {
        assertTrue(ErrorCode.AUDIT_STATUS_INVALID.getCode() >= 3000);
        assertTrue(ErrorCode.AUDIT_OPINION_REQUIRED.getCode() >= 3000);
        assertTrue(ErrorCode.ARCHIVE_NOT_FOUND.getCode() >= 3000);
    }
}
