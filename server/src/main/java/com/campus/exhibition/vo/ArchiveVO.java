package com.campus.exhibition.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 档案视图
 */
@Data
public class ArchiveVO {

    private Long id;
    private Long alumniId;
    private String alumniName;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String content;
    private LocalDate eventDate;
    private Long collegeId;
    private String collegeName;
    private String status;
    private Integer isTop;
    private Integer isRecommend;
    private Integer displaySort;
    private Long submitUser;
    private LocalDateTime submitTime;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;

    private List<MediaVO> mediaList;
}
