package com.campus.exhibition.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 校友/学生主表
 */
@Data
@TableName("alumni")
public class Alumni {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String studentNo;

    private String name;

    /** 1男 2女 */
    private Integer gender;

    private Long collegeId;

    private Long majorId;

    private Integer enrollYear;

    private Integer gradYear;

    /** 1在校生 2校友 */
    private Integer identity;

    private String avatar;

    private String summary;

    /** 人脸录入: 0未录 1已录 */
    private Integer faceStatus;

    /** 1正常 0停用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
