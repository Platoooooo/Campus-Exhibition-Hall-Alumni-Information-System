package com.campus.exhibition.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资料档案
 */
@Data
@TableName("archive")
public class Archive {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long alumniId;

    private Long categoryId;

    private String title;

    private String content;

    private LocalDate eventDate;

    /** 冗余学院，便于数据权限 */
    private Long collegeId;

    /** draft / pending_college / pending_academic / approved / rejected / published / unpublished */
    private String status;

    private Integer isTop;

    private Integer isRecommend;

    private Integer displaySort;

    private Long submitUser;

    private LocalDateTime submitTime;

    private LocalDateTime publishTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
