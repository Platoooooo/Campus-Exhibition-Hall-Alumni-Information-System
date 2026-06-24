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
        /** base64 编码的二进制特征（ArcSoft SDK 原始数据） */
        private String feature;
    }
}
