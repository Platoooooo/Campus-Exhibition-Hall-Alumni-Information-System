package com.campus.exhibition.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * /api/face/recognize 响应（契约字段名精确匹配）
 */
@Data
@Builder
public class FaceRecognizeVO {

    /** HIT / NO_MATCH / DEGRADED */
    private String status;

    private Long alumniId;
    private Float score;

    private AlumniBrief alumni;
    private List<TimelineItem> timeline;

    @Data
    @Builder
    public static class AlumniBrief {
        private Long id;
        private String name;
        private String avatar;
        private String collegeName;
        private Integer gradYear;
        private String summary;
    }

    @Data
    @Builder
    public static class TimelineItem {
        private Long archiveId;
        private String categoryName;
        private String title;
        private String eventDate;
        private String content;
        private List<MediaBrief> media;
    }

    @Data
    @Builder
    public static class MediaBrief {
        private int type;
        private String url;
        private String thumbnail;
        private Integer duration;
    }
}
