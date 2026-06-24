package com.campus.exhibition.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RoleEnum 角色枚举测试
 */
@DisplayName("RoleEnum 角色枚举")
class RoleEnumTest {

    @Test
    @DisplayName("包含三个角色")
    void threeRolesPresent() {
        assertEquals(3, RoleEnum.values().length);
    }

    @Test
    @DisplayName("角色编码唯一")
    void codesUnique() {
        Set<String> seen = new HashSet<>();
        for (RoleEnum r : RoleEnum.values()) {
            assertFalse(seen.contains(r.getCode()),
                    () -> "角色编码重复: " + r.getCode());
            seen.add(r.getCode());
        }
    }

    @Test
    @DisplayName("角色标签非空")
    void labelsNonBlank() {
        for (RoleEnum r : RoleEnum.values()) {
            assertNotNull(r.getLabel(), r.name() + " 标签不应为 null");
            assertFalse(r.getLabel().isBlank(), r.name() + " 标签不应为空");
        }
    }

    @Test
    @DisplayName("包含必需的三种角色")
    void requiredRolesExist() {
        Set<String> codes = Set.of("college", "academic", "admin");
        for (RoleEnum r : RoleEnum.values()) {
            codes.contains(r.getCode());
        }
        // 验证 each expected role exists
        assertNotNull(findByCode("college"), "缺少学院管理员角色");
        assertNotNull(findByCode("academic"), "缺少教务处管理员角色");
        assertNotNull(findByCode("admin"), "缺少校级管理员角色");
    }

    private RoleEnum findByCode(String code) {
        for (RoleEnum r : RoleEnum.values()) {
            if (r.getCode().equals(code)) return r;
        }
        return null;
    }
}
