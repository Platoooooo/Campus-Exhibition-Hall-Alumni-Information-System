package com.campus.exhibition.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 人脸识别日志
 */
@Data
@TableName("face_recog_log")
public class FaceRecogLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long alumniId;

    private Float score;

    /** 1命中 0未命中 */
    private Integer hit;

    private String device;

    private Integer costMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
