package com.campus.exhibition.dto;

import lombok.Data;

/**
 * 校友多条件分页查询
 */
@Data
public class AlumniQuery {

    private String name;
    private String studentNo;
    private Long collegeId;
    private Long majorId;
    private Integer gradYear;
    private Integer identity;
    private Integer status;
}
