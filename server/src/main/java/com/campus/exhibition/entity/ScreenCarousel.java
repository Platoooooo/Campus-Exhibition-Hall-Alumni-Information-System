package com.campus.exhibition.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 大屏轮播方案
 */
@Data
@TableName("screen_carousel")
public class ScreenCarousel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 单页停留秒数 */
    private Integer intervalSec;

    /** 动效: fade / slide / zoom */
    private String effect;

    /** 顺序: sort / random / time */
    private String orderType;

    /** 是否默认方案 */
    private Integer isDefault;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
