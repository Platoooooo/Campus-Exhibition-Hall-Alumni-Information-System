package com.campus.exhibition.dto;

import com.campus.exhibition.vo.UserVO;
import lombok.Builder;
import lombok.Data;

/**
 * 登录响应
 */
@Data
@Builder
public class LoginResponse {

    private String token;
    private UserVO user;
}
