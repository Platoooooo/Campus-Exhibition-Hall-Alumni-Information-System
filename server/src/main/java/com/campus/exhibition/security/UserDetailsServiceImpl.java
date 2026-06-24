package com.campus.exhibition.security;

import com.campus.exhibition.entity.SysUser;
import com.campus.exhibition.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security UserDetailsService 实现
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper sysUserMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );
        if (user == null) {
            throw new UsernameNotFoundException("账号不存在: " + username);
        }
        return new LoginUser(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getRole(),
                user.getCollegeId(),
                user.getStatus() != null && user.getStatus() == 1
        );
    }
}
