package com.campus.exhibition.dto.face;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * face-service /match 请求
 */
@Data
@Builder
public class FaceMatchRequest {

    private String image;
    private double threshold;
    private List<Candidate> candidates;

    @Data
    @Builder
    public static class Candidate {
        private long alumniId;
        private List<Float> feature;
    }
}
