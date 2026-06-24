package com.campus.exhibition.common;

import lombok.Getter;

/**
 * 统一错误码枚举
 */
@Getter
public enum ErrorCode {

    /* ---- 通用 1xxx ---- */
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    /* ---- 业务 2xxx ---- */
    USER_NOT_FOUND(2001, "账号不存在"),
    PASSWORD_ERROR(2002, "密码错误"),
    USER_DISABLED(2003, "账号已被禁用"),
    DUPLICATE_USERNAME(2004, "账号名已存在"),

    /* ---- 审核 3xxx ---- */
    AUDIT_STATUS_INVALID(3001, "当前状态不可审核"),
    AUDIT_OPINION_REQUIRED(3002, "驳回必须填写审核意见"),
    ARCHIVE_NOT_FOUND(3003, "资料档案不存在"),

    /* ---- 文件 4xxx ---- */
    FILE_UPLOAD_FAILED(4001, "文件上传失败"),
    FILE_TYPE_DENIED(4002, "不支持的文件类型"),
    FILE_SIZE_EXCEEDED(4003, "文件大小超出限制"),

    /* ---- 人脸 5xxx ---- */
    FACE_NOT_ENROLLED(5001, "未录入人脸特征"),
    FACE_MATCH_FAILED(5002, "人脸比对失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
