package com.campus.exhibition.security;

import com.campus.exhibition.entity.SysUser;
import com.campus.exhibition.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器 —— 从 Authorization 头提取 Bearer Token，解析并注入 SecurityContext
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final SysUserMapper sysUserMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtUtils.validateToken(token)) {
            Long userId = jwtUtils.getUserId(token);
            var claims = jwtUtils.parseToken(token);
            String role = claims.get("role", String.class);
            Long collegeId = claims.get("collegeId", Long.class);

            // 从 DB 确认用户仍存在且启用
            SysUser dbUser = sysUserMapper.selectById(userId);
            if (dbUser != null && dbUser.getStatus() != null && dbUser.getStatus() == 1) {
                LoginUser principal = new LoginUser(
                        userId,
                        dbUser.getUsername(),
                        "",
                        role,
                        collegeId,
                        true
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
