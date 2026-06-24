package com.campus.exhibition.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 系统用户创建/更新请求
 */
@Data
public class UserSaveRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 创建时必填，更新时可为空 */
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    @NotBlank(message = "角色不能为空")
    private String role;

    /** 学院管理员必填所属学院 */
    private Long collegeId;

    private String phone;

    private Integer status;
}
