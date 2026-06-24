package com.campus.exhibition.dto;

import lombok.Data;

/**
 * 学院查询条件
 */
@Data
public class CollegeQuery {

    private String name;
    private String code;
    private Integer status;
}
