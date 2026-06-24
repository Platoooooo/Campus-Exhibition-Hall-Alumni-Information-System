package com.campus.exhibition.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 专业视图
 */
@Data
public class MajorVO {

    private Long id;
    private Long collegeId;
    private String collegeName;
    private String name;
    private String code;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
}
