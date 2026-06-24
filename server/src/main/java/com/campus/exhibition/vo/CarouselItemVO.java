package com.campus.exhibition.vo;

import lombok.Data;

/**
 * 轮播项视图
 */
@Data
public class CarouselItemVO {

    private Long id;
    private Long carouselId;
    private Long archiveId;
    private String archiveTitle;
    private Long alumniId;
    private String alumniName;
    private Integer sort;
}
