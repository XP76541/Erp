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

@Service
@RequiredArgsConstructor
public class SystemAuthorizationService {
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;

    public void requireAdmin(TokenStore.LoginUser user) {
        if (user == null || user.userId() == null) throw new BusinessException(401, "未登录");
        SysUser current = userMapper.selectById(user.userId());
        if (current == null || current.getIsActive() == null || current.getIsActive() == 0)
            throw new BusinessException(403, "账号已被禁用");
        Set<Long> roleIds = new HashSet<>(userRoleMapper.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<com.erp.module.system.entity.SysUserRole>lambdaQuery()
                        .eq(com.erp.module.system.entity.SysUserRole::getUserId, user.userId()))
                .stream().map(com.erp.module.system.entity.SysUserRole::getRoleId).toList());
        if (roleIds.isEmpty() || roleMapper.selectBatchIds(roleIds).stream().noneMatch(r -> "ADMIN".equals(r.getCode())))
            throw new BusinessException(403, "无系统管理权限");
    }
}
