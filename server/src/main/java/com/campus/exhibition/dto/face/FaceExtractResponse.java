package com.campus.exhibition.dto.face;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * face-service /extract 响应（契约字段名精确匹配）
 */
@Data
public class FaceExtractResponse {

    private int code;
    private String message;

    @JsonProperty("data")
    private ExtractData data;

    @lombok.Data
    public static class ExtractData {
        private boolean faceFound;
        private int faceCount;
        /** base64 编码的二进制特征（ArcSoft SDK 原始数据，不可转为 float） */
        private String feature;
        private int dim;
        private Float quality;
        private String modelVer;
    }
}
