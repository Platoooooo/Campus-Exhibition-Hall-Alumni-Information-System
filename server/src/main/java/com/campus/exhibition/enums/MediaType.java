package com.campus.exhibition.enums;

import lombok.Getter;

/**
 * 媒体资源类型
 */
@Getter
public enum MediaType {

    IMAGE(1, "图片"),
    VIDEO(2, "视频"),
    DOCUMENT(3, "文档");

    private final int code;
    private final String label;

    MediaType(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static MediaType of(int code) {
        for (MediaType t : values()) {
            if (t.code == code) return t;
        }
        throw new IllegalArgumentException("未知媒体类型: " + code);
    }
}
