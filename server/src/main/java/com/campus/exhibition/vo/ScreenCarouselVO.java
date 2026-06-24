package com.campus.exhibition.vo;

import lombok.Data;

import java.util.List;

/**
 * 大屏轮播数据
 */
@Data
public class ScreenCarouselVO {

    private Long id;
    private String name;
    private Integer intervalSec;
    private String effect;
    private String orderType;
    private List<ScreenCarouselItem> items;

    @Data
    public static class ScreenCarouselItem {
        private Long id;
        private Long archiveId;
        private String title;
        private String content;
        private String eventDate;
        private Long alumniId;
        private String alumniName;
        private String alumniAvatar;
        private String gradYear;
        private String collegeName;
        private List<MediaItem> mediaList;
        private Integer sort;
    }

    @Data
    public static class MediaItem {
        private Long id;
        private Integer type;
        private String url;
        private String thumbnail;
    }
}
