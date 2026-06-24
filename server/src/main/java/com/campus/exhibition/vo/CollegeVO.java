package com.campus.exhibition.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学院视图
 */
@Data
public class CollegeVO {

    private Long id;
    private String name;
    private String code;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
}
