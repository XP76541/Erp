package com.erp.module.system.service;

import com.erp.common.BusinessException;
import com.erp.module.system.TokenStore;
import com.erp.module.system.entity.SysUser;
import com.erp.module.system.mapper.SysRoleMapper;
import com.erp.module.system.mapper.SysUserMapper;
import com.erp.module.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemAuthorizationService {
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;

    /** 允许价格/信用额度强制审核的高权限角色。角色实时从数据库读取。 */
    public boolean canForceSalesAudit(TokenStore.LoginUser user) {
        if (user == null || user.userId() == null) return false;
        SysUser current = userMapper.selectById(user.userId());
        if (current == null || current.getIsActive() == null || current.getIsActive() == 0) return false;
        Set<Long> roleIds = new HashSet<>(userRoleMapper.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<com.erp.module.system.entity.SysUserRole>lambdaQuery()
                        .eq(com.erp.module.system.entity.SysUserRole::getUserId, user.userId()))
                .stream().map(com.erp.module.system.entity.SysUserRole::getRoleId).toList());
        return !roleIds.isEmpty() && roleMapper.selectBatchIds(roleIds).stream()
                .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()) || "BOSS".equalsIgnoreCase(r.getCode()));
    }

    public void requireAdmin(TokenStore.LoginUser user) {
        if (!hasRole(user, "ADMIN")) throw new BusinessException(403, "无系统管理权限");
    }

    /**
     * 返回当前用户的角色编码。每次从数据库解析，避免登录快照中的角色过期。
     */
    public Set<String> roleCodes(TokenStore.LoginUser user) {
        if (user == null || user.userId() == null) throw new BusinessException(401, "未登录");
        SysUser current = userMapper.selectById(user.userId());
        if (current == null || current.getIsActive() == null || current.getIsActive() == 0)
            throw new BusinessException(403, "账号已被禁用");
        Set<Long> roleIds = new HashSet<>(userRoleMapper.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<com.erp.module.system.entity.SysUserRole>lambdaQuery()
                        .eq(com.erp.module.system.entity.SysUserRole::getUserId, user.userId()))
                .stream().map(com.erp.module.system.entity.SysUserRole::getRoleId).toList());
        return roleIds.isEmpty() ? Set.of() : roleMapper.selectBatchIds(roleIds).stream()
                .map(r -> r.getCode()).collect(Collectors.toSet());
    }

    public boolean hasRole(TokenStore.LoginUser user, String roleCode) {
        return roleCodes(user).contains(roleCode);
    }

    /** SALES users are scoped to their own salesperson id; other approved roles are unrestricted. */
    public Long salespersonScope(TokenStore.LoginUser user) {
        return hasRole(user, "SALES") ? user.userId() : null;
    }

    public void requireUnrestrictedOrSalesperson(TokenStore.LoginUser user, Long salespersonId) {
        Long scope = salespersonScope(user);
        if (scope != null && !scope.equals(salespersonId)) {
            throw new BusinessException(403, "无权访问其他销售人员的单据");
        }
    }
}
