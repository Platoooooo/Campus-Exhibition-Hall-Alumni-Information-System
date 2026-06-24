package com.campus.exhibition.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 人脸特征库
 */
@Data
@TableName("face_feature")
public class FaceFeature {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long alumniId;

    private byte[] feature;

    private String modelVer;

    private Float quality;

    /** 1有效 0失效 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
