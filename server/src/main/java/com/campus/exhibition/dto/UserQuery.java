package com.campus.exhibition.dto;

import lombok.Data;

/**
 * 用户查询条件
 */
@Data
public class UserQuery {

    private String username;
    private String realName;
    private String role;
    private Long collegeId;
    private Integer status;
}
