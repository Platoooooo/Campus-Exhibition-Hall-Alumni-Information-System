package com.campus.exhibition.dto.face;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * face-service /match 响应（契约字段名精确匹配）
 */
@Data
public class FaceMatchResponse {

    private int code;
    private String message;

    @JsonProperty("data")
    private MatchData data;

    @lombok.Data
    public static class MatchData {
        private boolean faceFound;
        private boolean hit;
        private Long alumniId;
        private Float score;
        private double threshold;
        private String modelVer;
    }
}
