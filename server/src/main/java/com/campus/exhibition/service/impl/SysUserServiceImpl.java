package com.campus.exhibition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exhibition.common.BizException;
import com.campus.exhibition.common.ErrorCode;
import com.campus.exhibition.dto.ResetPasswordRequest;
import com.campus.exhibition.dto.UserQuery;
import com.campus.exhibition.dto.UserSaveRequest;
import com.campus.exhibition.entity.SysCollege;
import com.campus.exhibition.entity.SysUser;
import com.campus.exhibition.mapper.SysCollegeMapper;
import com.campus.exhibition.mapper.SysUserMapper;
import com.campus.exhibition.service.SysUserService;
import com.campus.exhibition.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper userMapper;
    private final SysCollegeMapper collegeMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<UserVO> page(UserQuery query, int pageNum, int pageSize) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        if (query != null) {
            w.like(StringUtils.hasText(query.getUsername()), SysUser::getUsername, query.getUsername());
            w.like(StringUtils.hasText(query.getRealName()), SysUser::getRealName, query.getRealName());
            w.eq(StringUtils.hasText(query.getRole()), SysUser::getRole, query.getRole());
            w.eq(query.getCollegeId() != null, SysUser::getCollegeId, query.getCollegeId());
            w.eq(query.getStatus() != null, SysUser::getStatus, query.getStatus());
        }
        w.orderByAsc(SysUser::getCreateTime);

        Page<SysUser> page = userMapper.selectPage(Page.of(pageNum, pageSize), w);
        Page<UserVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public UserVO getById(Long id) {
        SysUser entity = userMapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        return toVO(entity);
    }

    @Override
    @Transactional
    public UserVO create(UserSaveRequest request) {
        // 用户名唯一校验
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()));
        if (count > 0) throw new BizException(400, "用户名已存在");

        // 学院管理员必须有 collegeId
        if ("college".equals(request.getRole()) && request.getCollegeId() == null) {
            throw new BizException(400, "学院管理员必须指定所属学院");
        }
        if (request.getCollegeId() != null) {
            if (collegeMapper.selectById(request.getCollegeId()) == null) {
                throw new BizException(400, "学院不存在");
            }
        }

        SysUser entity = new SysUser();
        BeanUtils.copyProperties(request, entity);
        entity.setPassword(passwordEncoder.encode(
                request.getPassword() != null ? request.getPassword() : "123456"));
        if (entity.getStatus() == null) entity.setStatus(1);
        userMapper.insert(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public UserVO update(Long id, UserSaveRequest request) {
        SysUser entity = userMapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");

        // 用户名校验（排己）
        if (!entity.getUsername().equals(request.getUsername())) {
            Long count = userMapper.selectCount(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getUsername, request.getUsername())
                            .ne(SysUser::getId, id));
            if (count > 0) throw new BizException(400, "用户名已存在");
        }

        if ("college".equals(request.getRole()) && request.getCollegeId() == null) {
            throw new BizException(400, "学院管理员必须指定所属学院");
        }

        BeanUtils.copyProperties(request, entity);
        if (StringUtils.hasText(request.getPassword())) {
            entity.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        userMapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (userMapper.selectById(id) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        // 不允许删除自己
        Long selfId = com.campus.exhibition.common.UserContext.userId();
        if (selfId != null && selfId.equals(id)) {
            throw new BizException(400, "不能删除自己的账号");
        }
        userMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest request) {
        SysUser entity = userMapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        entity.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(entity);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        SysUser entity = userMapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        entity.setStatus(entity.getStatus() == 1 ? 0 : 1);
        userMapper.updateById(entity);
    }

    private UserVO toVO(SysUser entity) {
        UserVO vo = new UserVO();
        vo.setId(entity.getId());
        vo.setUsername(entity.getUsername());
        vo.setRealName(entity.getRealName());
        vo.setRole(entity.getRole());
        vo.setCollegeId(entity.getCollegeId());
        vo.setPhone(entity.getPhone());
        vo.setStatus(entity.getStatus());
        return vo;
    }
}
