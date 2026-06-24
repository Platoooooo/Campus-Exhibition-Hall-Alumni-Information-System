package com.campus.exhibition.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 轮播项（轮播池内容）
 */
@Data
@TableName("screen_carousel_item")
public class ScreenCarouselItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long carouselId;

    /** 关联档案 */
    private Long archiveId;

    /** 关联校友（整人轮播） */
    private Long alumniId;

    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
