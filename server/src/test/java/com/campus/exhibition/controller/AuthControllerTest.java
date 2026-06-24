package com.campus.exhibition.controller;

import com.campus.exhibition.common.GlobalExceptionHandler;
import com.campus.exhibition.entity.SysUser;
import com.campus.exhibition.mapper.SysLoginLogMapper;
import com.campus.exhibition.mapper.SysUserMapper;
import com.campus.exhibition.security.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuthController 认证接口")
class AuthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    private SysUserMapper sysUserMapper;
    private SysLoginLogMapper loginLogMapper;
    private PasswordEncoder passwordEncoder;
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        sysUserMapper = mock(SysUserMapper.class);
        loginLogMapper = mock(SysLoginLogMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtUtils = mock(JwtUtils.class);

        AuthController controller = new AuthController(sysUserMapper, loginLogMapper, passwordEncoder, jwtUtils);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("登录成功返回 token 和用户信息")
    void login_success() throws Exception {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("$2a$encoded");
        user.setRole("admin");
        user.setCollegeId(null);
        user.setStatus(1);

        when(sysUserMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("admin123", user.getPassword())).thenReturn(true);
        when(jwtUtils.generateToken(1L, "admin", null)).thenReturn("jwt-token-xxx");

        var req = new com.campus.exhibition.dto.LoginRequest();
        req.setUsername("admin");
        req.setPassword("admin123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("jwt-token-xxx"))
                .andExpect(jsonPath("$.data.user.username").value("admin"))
                .andExpect(jsonPath("$.data.user.role").value("admin"));
    }

    @Test
    @DisplayName("用户不存在返回 2001")
    void login_userNotFound() throws Exception {
        when(sysUserMapper.selectOne(any())).thenReturn(null);

        var req = new com.campus.exhibition.dto.LoginRequest();
        req.setUsername("nobody");
        req.setPassword("x");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001));
    }

    @Test
    @DisplayName("密码错误返回 2002")
    void login_passwordError() throws Exception {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("$2a$encoded");
        user.setStatus(1);

        when(sysUserMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("wrong", user.getPassword())).thenReturn(false);

        var req = new com.campus.exhibition.dto.LoginRequest();
        req.setUsername("admin");
        req.setPassword("wrong");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002));
    }

    @Test
    @DisplayName("账号禁用返回 2003")
    void login_userDisabled() throws Exception {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("$2a$encoded");
        user.setStatus(0);

        when(sysUserMapper.selectOne(any())).thenReturn(user);

        var req = new com.campus.exhibition.dto.LoginRequest();
        req.setUsername("admin");
        req.setPassword("admin123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2003));
    }
}
