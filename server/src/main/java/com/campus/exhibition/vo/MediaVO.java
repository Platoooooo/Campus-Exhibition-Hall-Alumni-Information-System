package com.campus.exhibition.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 媒体视图
 */
@Data
public class MediaVO {

    private Long id;
    private Long archiveId;
    private Integer type;
    private String url;
    private String thumbnail;
    private String fileName;
    private Long fileSize;
    private Integer duration;
    private Integer sort;
    private LocalDateTime createTime;
}
