package com.campus.exhibition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 校友创建/更新请求
 */
@Data
public class AlumniSaveRequest {

    @NotBlank(message = "学号不能为空")
    private String studentNo;

    @NotBlank(message = "姓名不能为空")
    private String name;

    private Integer gender;

    @NotNull(message = "学院不能为空")
    private Long collegeId;

    private Long majorId;

    private Integer enrollYear;

    private Integer gradYear;

    /** 1在校生 2校友 */
    private Integer identity;

    private String avatar;

    private String summary;
}
