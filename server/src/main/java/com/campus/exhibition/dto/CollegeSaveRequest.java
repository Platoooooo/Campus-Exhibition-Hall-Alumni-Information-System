package com.campus.exhibition.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 学院创建/更新请求
 */
@Data
public class CollegeSaveRequest {

    @NotBlank(message = "学院名称不能为空")
    private String name;

    @NotBlank(message = "学院编码不能为空")
    private String code;

    private Integer sort;
    private Integer status;
}
