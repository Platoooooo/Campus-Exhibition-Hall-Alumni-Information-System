package com.campus.exhibition.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志
 */
@Data
@TableName("sys_oper_log")
public class SysOperLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作描述 */
    private String description;

    /** 请求方法: GET / POST / PUT / DELETE */
    private String method;

    /** 请求 URI */
    private String uri;

    /** 操作人 ID */
    private Long userId;

    /** 操作人用户名 */
    private String username;

    /** 操作人 IP */
    private String ip;

    /** 请求参数（已过滤密码/人脸） */
    private String params;

    /** 耗时 ms */
    private Long costMs;

    /** 0成功 1异常 */
    private Integer status;

    /** 异常信息 */
    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
