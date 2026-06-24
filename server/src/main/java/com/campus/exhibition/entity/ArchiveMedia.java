package com.campus.exhibition.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 档案媒体资源
 */
@Data
@TableName("archive_media")
public class ArchiveMedia {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long archiveId;

    /** 1图片 2视频 3文档 */
    private Integer type;

    private String url;

    private String thumbnail;

    private String fileName;

    private Long fileSize;

    /** 视频时长（秒） */
    private Integer duration;

    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
