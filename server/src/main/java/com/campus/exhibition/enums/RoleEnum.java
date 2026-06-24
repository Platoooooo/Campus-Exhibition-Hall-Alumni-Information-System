package com.campus.exhibition.enums;

import lombok.Getter;

/**
 * 系统角色
 */
@Getter
public enum RoleEnum {

    COLLEGE("college", "学院管理员"),
    ACADEMIC("academic", "教务处管理员"),
    ADMIN("admin", "校级管理员");

    private final String code;
    private final String label;

    RoleEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
