package com.campus.exhibition.dto;

import lombok.Data;

/**
 * 专业查询条件
 */
@Data
public class MajorQuery {

    private Long collegeId;
    private String name;
    private String code;
    private Integer status;
}
