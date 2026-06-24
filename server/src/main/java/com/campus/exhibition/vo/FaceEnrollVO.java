package com.campus.exhibition.vo;

import lombok.Builder;
import lombok.Data;

/**
 * /api/face/enroll 响应
 */
@Data
@Builder
public class FaceEnrollVO {

    private Long alumniId;
    private Float quality;
    private String modelVer;
    private Integer faceStatus;
}
