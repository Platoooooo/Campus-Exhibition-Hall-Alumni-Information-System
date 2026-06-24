package com.campus.exhibition.controller;

import com.campus.exhibition.common.BizException;
import com.campus.exhibition.common.ErrorCode;
import com.campus.exhibition.common.R;
import com.campus.exhibition.dto.LoginRequest;
import com.campus.exhibition.dto.LoginResponse;
import com.campus.exhibition.entity.SysLoginLog;
import com.campus.exhibition.entity.SysUser;
import com.campus.exhibition.mapper.SysLoginLogMapper;
import com.campus.exhibition.mapper.SysUserMapper;
import com.campus.exhibition.security.JwtUtils;
import com.campus.exhibition.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserMapper sysUserMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpReq) {
        SysLoginLog loginLog = new SysLoginLog();
        loginLog.setUsername(request.getUsername());
        loginLog.setIp(getIp(httpReq));

        try {
            // 查用户
            SysUser user = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getUsername, request.getUsername())
            );
            if (user == null) {
                loginLog.setStatus(0);
                loginLog.setFailReason("用户不存在");
                loginLogMapper.insert(loginLog);
                throw new BizException(ErrorCode.USER_NOT_FOUND);
            }
            if (user.getStatus() == null || user.getStatus() != 1) {
                loginLog.setUserId(user.getId());
                loginLog.setStatus(0);
                loginLog.setFailReason("账号已禁用");
                loginLogMapper.insert(loginLog);
                throw new BizException(ErrorCode.USER_DISABLED);
            }
            // BCrypt 密码校验
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                loginLog.setUserId(user.getId());
                loginLog.setStatus(0);
                loginLog.setFailReason("密码错误");
                loginLogMapper.insert(loginLog);
                throw new BizException(ErrorCode.PASSWORD_ERROR);
            }

            // 登录成功
            loginLog.setUserId(user.getId());
            loginLog.setStatus(1);
            loginLogMapper.insert(loginLog);

            // 生成 JWT
            String token = jwtUtils.generateToken(user.getId(), user.getRole(), user.getCollegeId());

            // 更新最后登录时间
            user.setLastLogin(java.time.LocalDateTime.now());
            sysUserMapper.updateById(user);

            UserVO userVO = UserVO.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .realName(user.getRealName())
                    .role(user.getRole())
                    .collegeId(user.getCollegeId())
                    .phone(user.getPhone())
                    .build();

            return R.ok(LoginResponse.builder().token(token).user(userVO).build());
        } catch (BizException e) {
            throw e;
        }
    }

    private static String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip;
    }
}
