package com.campus.exhibition.dto;

import lombok.Data;

/**
 * 档案查询条件
 */
@Data
public class ArchiveQuery {

    private Long alumniId;
    private Long categoryId;
    private String status;
    private String title;
    private Long collegeId;
    private Integer isRecommend;
}
