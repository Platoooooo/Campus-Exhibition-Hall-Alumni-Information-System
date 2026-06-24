package com.campus.exhibition.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 大屏搜索结果
 */
@Data
public class ScreenSearchResultVO {

    private Long archiveId;
    private String title;
    private String content;
    private LocalDate eventDate;
    private String categoryName;
    private Long alumniId;
    private String alumniName;
    private String alumniAvatar;
    private String collegeName;
    private List<ScreenCarouselVO.MediaItem> mediaList;
}
