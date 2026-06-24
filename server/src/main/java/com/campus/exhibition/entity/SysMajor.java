package com.campus.exhibition.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 专业字典
 */
@Data
@TableName("sys_major")
public class SysMajor {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long collegeId;

    private String name;

    private String code;

    private Integer sort;

    /** 1启用 0停用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
