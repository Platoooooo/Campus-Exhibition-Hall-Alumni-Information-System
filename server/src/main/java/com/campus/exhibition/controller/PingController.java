package com.campus.exhibition.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.exhibition.common.R;
import com.campus.exhibition.common.UserContext;
import com.campus.exhibition.entity.SysUser;
import com.campus.exhibition.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 探活接口 —— 验证整链路（Controller → Mapper → DB）连通，需认证
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PingController {

    private final SysUserMapper sysUserMapper;

    @GetMapping("/ping")
    @PreAuthorize("hasAuthority('admin') or hasAuthority('academic') or hasAuthority('college')")
    public R<Map<String, Object>> ping() {
        long userCount = sysUserMapper.selectCount(new LambdaQueryWrapper<>());
        List<SysUser> users = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .select(SysUser::getId, SysUser::getUsername, SysUser::getRealName, SysUser::getRole)
                        .orderByAsc(SysUser::getId)
                        .last("LIMIT 5")
        );

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("service", "campus-exhibition");
        info.put("db", "connected");
        info.put("userCount", userCount);
        info.put("users", users);
        info.put("currentUser", UserContext.currentUser() != null ?
                UserContext.currentUser().getUsername() : "anonymous");
        info.put("timestamp", LocalDateTime.now());

        return R.ok(info);
    }
}
