package com.campus.exhibition.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资料分类
 */
@Data
@TableName("archive_category")
public class ArchiveCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父级，0为根 */
    private Long parentId;

    private String name;

    private String icon;

    private Integer sort;

    /** 1启用 0停用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
