package com.campus.exhibition.dto.face;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 大屏识别请求（Spring Boot 对外接口）
 */
@Data
public class FaceRecognizeRequest {

    @NotBlank(message = "图像不能为空")
    private String image;

    /** 一体机标识 */
    private String device;
}
