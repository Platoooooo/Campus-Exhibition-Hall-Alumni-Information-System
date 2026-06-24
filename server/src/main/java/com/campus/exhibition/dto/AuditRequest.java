package com.campus.exhibition.dto;

import lombok.Data;

/**
 * 审核操作请求
 */
@Data
public class AuditRequest {

    /** 审核意见（驳回时必填） */
    private String opinion;
}
