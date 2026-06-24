package com.campus.exhibition.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exhibition.common.R;
import com.campus.exhibition.entity.SysLoginLog;
import com.campus.exhibition.entity.SysOperLog;
import com.campus.exhibition.mapper.SysLoginLogMapper;
import com.campus.exhibition.mapper.SysOperLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 日志查询（仅 admin）
 */
@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('admin')")
public class LogController {

    private final SysOperLogMapper operLogMapper;
    private final SysLoginLogMapper loginLogMapper;

    /** 操作日志分页 */
    @GetMapping("/oper")
    public R<Page<SysOperLog>> operLog(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String username) {
        LambdaQueryWrapper<SysOperLog> w = new LambdaQueryWrapper<>();
        if (username != null && !username.isBlank()) {
            w.eq(SysOperLog::getUsername, username);
        }
        w.orderByDesc(SysOperLog::getCreateTime);
        return R.ok(operLogMapper.selectPage(Page.of(pageNum, pageSize), w));
    }

    /** 登录日志分页 */
    @GetMapping("/login")
    public R<Page<SysLoginLog>> loginLog(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String username) {
        LambdaQueryWrapper<SysLoginLog> w = new LambdaQueryWrapper<>();
        if (username != null && !username.isBlank()) {
            w.eq(SysLoginLog::getUsername, username);
        }
        w.orderByDesc(SysLoginLog::getCreateTime);
        return R.ok(loginLogMapper.selectPage(Page.of(pageNum, pageSize), w));
    }
}
