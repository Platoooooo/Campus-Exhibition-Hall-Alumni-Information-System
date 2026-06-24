package com.campus.exhibition.vo;

import lombok.Data;

/**
 * 校友墙卡片
 */
@Data
public class WallItemVO {

    private Long alumniId;
    private String name;
    private String avatar;
    private Integer gradYear;
    private String collegeName;
    /** 代表标签（取该校友第一条 published 档案标题） */
    private String tag;
    /** 代表作品缩略图 */
    private String thumbnail;
}
