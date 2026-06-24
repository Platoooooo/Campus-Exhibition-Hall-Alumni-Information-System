package com.campus.exhibition.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 校友视图
 */
@Data
public class AlumniVO {

    private Long id;
    private String studentNo;
    private String name;
    private Integer gender;
    private Long collegeId;
    private String collegeName;
    private Long majorId;
    private String majorName;
    private Integer enrollYear;
    private Integer gradYear;
    private Integer identity;
    private String avatar;
    private String summary;
    private Integer faceStatus;
    private Integer status;
    private LocalDateTime createTime;
}
