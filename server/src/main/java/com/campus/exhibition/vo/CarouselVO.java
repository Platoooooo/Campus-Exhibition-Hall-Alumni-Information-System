package com.campus.exhibition.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 轮播方案视图
 */
@Data
public class CarouselVO {

    private Long id;
    private String name;
    private Integer intervalSec;
    private String effect;
    private String orderType;
    private Integer isDefault;
    private Integer status;
    private LocalDateTime createTime;

    private List<CarouselItemVO> items;
}
