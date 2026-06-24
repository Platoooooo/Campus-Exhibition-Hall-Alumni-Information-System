package com.campus.exhibition.dto;

import lombok.Data;

/**
 * 轮播项添加请求
 */
@Data
public class CarouselItemRequest {

    private Long archiveId;
    private Long alumniId;
    private Integer sort;
}
