package com.campus.exhibition.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 登录用户信息（存入 SecurityContext）
 */
@Getter
public class LoginUser implements UserDetails {

    private final Long userId;
    private final String username;
    private final String role;
    private final Long collegeId;
    private final String password;
    private final boolean enabled;

    public LoginUser(Long userId, String username, String password,
                     String role, Long collegeId, boolean enabled) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.role = role;
        this.collegeId = collegeId;
        this.enabled = enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return enabled; }
}
