package com.campus.exhibition.common;

import java.lang.annotation.*;

/**
 * 操作日志注解 —— 打在 Controller 方法上自动记录操作日志
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLog {

    /** 操作描述，如 "新增校友" */
    String value();
}
