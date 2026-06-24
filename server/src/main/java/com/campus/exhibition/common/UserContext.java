package com.campus.exhibition.common;

import com.campus.exhibition.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户上下文工具
 */
public final class UserContext {

    private UserContext() {}

    /**
     * 获取当前登录用户
     */
    public static LoginUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof LoginUser) {
            return (LoginUser) principal;
        }
        return null;
    }

    public static Long userId() {
        LoginUser user = currentUser();
        return user != null ? user.getUserId() : null;
    }

    public static String role() {
        LoginUser user = currentUser();
        return user != null ? user.getRole() : null;
    }

    public static Long collegeId() {
        LoginUser user = currentUser();
        return user != null ? user.getCollegeId() : null;
    }
}
