package com.campus.exhibition.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exhibition.dto.ResetPasswordRequest;
import com.campus.exhibition.dto.UserQuery;
import com.campus.exhibition.dto.UserSaveRequest;
import com.campus.exhibition.vo.UserVO;

/**
 * 系统用户管理
 */
public interface SysUserService {

    Page<UserVO> page(UserQuery query, int pageNum, int pageSize);

    UserVO getById(Long id);

    UserVO create(UserSaveRequest request);

    UserVO update(Long id, UserSaveRequest request);

    void delete(Long id);

    void resetPassword(Long id, ResetPasswordRequest request);

    void toggleStatus(Long id);
}
