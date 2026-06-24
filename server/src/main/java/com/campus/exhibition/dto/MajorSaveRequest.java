package com.campus.exhibition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 专业创建/更新请求
 */
@Data
public class MajorSaveRequest {

    @NotNull(message = "所属学院不能为空")
    private Long collegeId;

    @NotBlank(message = "专业名称不能为空")
    private String name;

    private String code;
    private Integer sort;
    private Integer status;
}
