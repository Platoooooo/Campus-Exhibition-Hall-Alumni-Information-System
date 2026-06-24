package com.campus.exhibition.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审核记录视图
 */
@Data
public class AuditLogVO {

    private Long id;
    private Long archiveId;
    private String archiveTitle;
    private String node;
    private String action;
    private String opinion;
    private Long auditorId;
    private String auditorName;
    private LocalDateTime createTime;
}
