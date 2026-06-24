package com.campus.exhibition.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志
 */
@Data
@TableName("sys_login_log")
public class SysLoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    private String ip;

    /** 0失败 1成功 */
    private Integer status;

    /** 失败原因 */
    private String failReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
