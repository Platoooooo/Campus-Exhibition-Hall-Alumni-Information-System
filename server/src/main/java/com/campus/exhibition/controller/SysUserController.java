package com.campus.exhibition.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exhibition.common.OperLog;
import com.campus.exhibition.common.R;
import com.campus.exhibition.dto.ResetPasswordRequest;
import com.campus.exhibition.dto.UserQuery;
import com.campus.exhibition.dto.UserSaveRequest;
import com.campus.exhibition.service.SysUserService;
import com.campus.exhibition.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 系统用户管理（仅 admin）
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('admin')")
public class SysUserController {

    private final SysUserService userService;

    @GetMapping
    @OperLog("查询用户列表")
    public R<Page<UserVO>> page(
            UserQuery query,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(userService.page(query, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public R<UserVO> getById(@PathVariable Long id) {
        return R.ok(userService.getById(id));
    }

    @PostMapping
    @OperLog("新增用户")
    public R<UserVO> create(@Valid @RequestBody UserSaveRequest request) {
        return R.ok(userService.create(request));
    }

    @PutMapping("/{id}")
    @OperLog("编辑用户")
    public R<UserVO> update(@PathVariable Long id, @Valid @RequestBody UserSaveRequest request) {
        return R.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @OperLog("删除用户")
    public R<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return R.ok();
    }

    @PutMapping("/{id}/reset-password")
    @OperLog("重置密码")
    public R<Void> resetPassword(@PathVariable Long id,
                                  @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request);
        return R.ok();
    }

    @PutMapping("/{id}/toggle")
    @OperLog("启停用户")
    public R<Void> toggleStatus(@PathVariable Long id) {
        userService.toggleStatus(id);
        return R.ok();
    }
}
