package com.campus.exhibition.dto;

import lombok.Data;

/**
 * 大屏搜索条件
 */
@Data
public class ScreenSearchQuery {

    private String keyword;
    private Long collegeId;
    private Integer gradYear;
    private Long categoryId;
}
