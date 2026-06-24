package com.campus.exhibition.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 校友成长轨迹（大屏详情页）
 */
@Data
public class AlumniTimelineVO {

    private Long alumniId;
    private String name;
    private String avatar;
    private String summary;
    private Integer gradYear;
    private String collegeName;
    private List<TimelineNode> timeline;

    @Data
    public static class TimelineNode {
        private Long archiveId;
        private String title;
        private String content;
        private LocalDate eventDate;
        private String categoryName;
        private List<ScreenCarouselVO.MediaItem> mediaList;
    }
}
