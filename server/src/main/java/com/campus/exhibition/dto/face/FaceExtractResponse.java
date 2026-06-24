package com.campus.exhibition.dto.face;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

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
        private List<Float> feature;
        private int dim;
        private Float quality;
        private String modelVer;
    }
}
