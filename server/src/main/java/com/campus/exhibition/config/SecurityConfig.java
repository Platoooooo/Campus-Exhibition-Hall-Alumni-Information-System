package com.campus.exhibition.config;

import com.campus.exhibition.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置 —— JWT 无状态会话 + RBAC 方法级鉴权
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // 启用 @PreAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 认证接口公开
                .requestMatchers("/api/auth/**").permitAll()
                // 大屏公开接口（匿名可访问）
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/screen/**").permitAll()
                // 大屏人脸识别匿名
                .requestMatchers("/api/face/recognize").permitAll()
                // 上传文件公开访问
                .requestMatchers("/uploads/**").permitAll()
                // 健康检查公开
                .requestMatchers("/actuator/health").permitAll()
                // OPTIONS 预检放行
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 其余接口均需认证
                .anyRequest().authenticated()
            )
            // 无状态，禁用表单登录/HTTP Basic
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            // 添加 JWT 过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
