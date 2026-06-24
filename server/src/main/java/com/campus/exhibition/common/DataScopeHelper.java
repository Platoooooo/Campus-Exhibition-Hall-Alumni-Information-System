package com.campus.exhibition.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.campus.exhibition.enums.RoleEnum;

/**
 * 数据权限工具 —— 学院管理员查询时自动注入 college_id 条件
 *
 * <pre>
 * 用法示例：
 *   LambdaQueryWrapper&lt;Archive&gt; wrapper = new LambdaQueryWrapper&lt;&gt;();
 *   DataScopeHelper.applyCollegeScope(wrapper, Archive::getCollegeId);
 *   // 学院管理员 → wrapper 自动增加 .eq(Archive::getCollegeId, 当前用户的collegeId)
 *   // 教务处/校级 → wrapper 不变
 * </pre>
 */
public final class DataScopeHelper {

    private DataScopeHelper() {}

    /**
     * 对查询包装器注入学院数据隔离条件。
     * 仅当当前用户角色为 college 时生效；academic / admin 不做限制。
     *
     * @param wrapper       查询包装器
     * @param collegeColumn 实体中代表学院 ID 的字段（如 Archive::getCollegeId）
     * @param <T>           实体类型
     */
    public static <T> void applyCollegeScope(LambdaQueryWrapper<T> wrapper,
                                             SFunction<T, ?> collegeColumn) {
        String role = UserContext.role();
        if (role == null) return;

        if (RoleEnum.COLLEGE.getCode().equals(role)) {
            Long collegeId = UserContext.collegeId();
            if (collegeId != null) {
                wrapper.eq(collegeColumn, collegeId);
            }
        }
        // academic 和 admin 不限制
    }

    /**
     * 获取当前用户可视的学院 ID：
     * 学院管理员 → 返回其所属 collegeId；教务处/校级 → 返回 null（表示不限）
     */
    public static Long visibleCollegeId() {
        String role = UserContext.role();
        if (role == null) return null;
        if (RoleEnum.COLLEGE.getCode().equals(role)) {
            return UserContext.collegeId();
        }
        return null; // 教务处/校级不限制
    }

    /**
     * 判断当前用户角色是否可查看全校数据
     */
    public static boolean isAllColleges() {
        String role = UserContext.role();
        return RoleEnum.ACADEMIC.getCode().equals(role)
                || RoleEnum.ADMIN.getCode().equals(role);
    }
}
