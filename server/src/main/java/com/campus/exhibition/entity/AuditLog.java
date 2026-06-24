package com.campus.exhibition.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审核记录
 */
@Data
@TableName("audit_log")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long archiveId;

    /** 环节: college / academic */
    private String node;

    /** 动作: approve / reject */
    private String action;

    /** 审核意见（驳回必填） */
    private String opinion;

    private Long auditorId;

    private String auditorName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
