package com.erp.module.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.module.system.entity.*;
import com.erp.module.system.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemManagementService {
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final PasswordEncoder passwordEncoder;

    public record UserView(Long id, String username, String realName, Integer isActive, List<Long> roleIds) {}
    public record RoleView(SysRole role, List<Long> permissionIds) {}

    public PageResult<UserView> users(long page, long size, String keyword, Integer active) {
        var q = Wrappers.<SysUser>lambdaQuery().and(keyword != null && !keyword.isBlank(), w ->
                w.like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword))
                .eq(active != null, SysUser::getIsActive, active).orderByDesc(SysUser::getId);
        Page<SysUser> result = userMapper.selectPage(new Page<>(page, size), q);
        return PageResult.of(result.getTotal(), result.getRecords().stream().map(this::toView).toList());
    }

    private UserView toView(SysUser u) {
        List<Long> roles = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, u.getId()))
                .stream().map(SysUserRole::getRoleId).toList();
        return new UserView(u.getId(), u.getUsername(), u.getRealName(), u.getIsActive(), roles);
    }

    @Transactional
    public Long createUser(String username, String realName, String password, Integer active, List<Long> roleIds) {
        if (userMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username)) > 0)
            throw new BusinessException("用户名已存在");
        SysUser u = new SysUser(); u.setUsername(username); u.setRealName(realName); u.setPasswordHash(passwordEncoder.encode(password)); u.setIsActive(active == null ? 1 : active);
        userMapper.insert(u); replaceUserRoles(u.getId(), roleIds); return u.getId();
    }

    @Transactional
    public void updateUser(Long id, String realName, Integer active, List<Long> roleIds) {
        SysUser u = mustUser(id); if (realName != null) u.setRealName(realName); if (active != null) u.setIsActive(active);
        if (u.getIsActive() == 0 && isLastActiveAdmin(id, roleIds)) throw new BusinessException("不能停用最后一个管理员");
        userMapper.updateById(u); if (roleIds != null) replaceUserRoles(id, roleIds);
    }

    @Transactional
    public void resetPassword(Long id, String password) { SysUser u = mustUser(id); u.setPasswordHash(passwordEncoder.encode(password)); userMapper.updateById(u); }

    private SysUser mustUser(Long id) { SysUser u = userMapper.selectById(id); if (u == null) throw new BusinessException("用户不存在"); return u; }
    private boolean isLastActiveAdmin(Long id, List<Long> requested) {
        List<Long> ids = requested == null ? userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id)).stream().map(SysUserRole::getRoleId).toList() : requested;
        Long adminRole = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, "ADMIN")).getId();
        if (!ids.contains(adminRole)) return false;
        long count = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getIsActive, 1));
        return count <= 1;
    }
    private void replaceUserRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId));
        if (roleIds != null) for (Long roleId : roleIds) { if (roleMapper.selectById(roleId) == null) throw new BusinessException("角色不存在:" + roleId); SysUserRole x = new SysUserRole(); x.setUserId(userId); x.setRoleId(roleId); userRoleMapper.insert(x); }
    }

    public List<RoleView> roles() { return roleMapper.selectList(Wrappers.<SysRole>query().orderByAsc("id")).stream().map(r -> new RoleView(r, rolePermissionMapper.selectList(Wrappers.<SysRolePermission>lambdaQuery().eq(SysRolePermission::getRoleId, r.getId())).stream().map(SysRolePermission::getPermissionId).toList())).toList(); }
    public List<SysPermission> permissions() { return permissionMapper.selectList(Wrappers.<SysPermission>query().orderByAsc("parent_id").orderByAsc("sort").orderByAsc("id")); }
    @Transactional public void updateRole(Long id, String name, String remark, List<Long> permissionIds) { SysRole r=roleMapper.selectById(id); if(r==null) throw new BusinessException("角色不存在"); r.setName(name); r.setRemark(remark); roleMapper.updateById(r); rolePermissionMapper.delete(Wrappers.<SysRolePermission>lambdaQuery().eq(SysRolePermission::getRoleId,id)); if(permissionIds!=null) for(Long p:permissionIds){if(permissionMapper.selectById(p)==null)throw new BusinessException("权限不存在:"+p); SysRolePermission x=new SysRolePermission();x.setRoleId(id);x.setPermissionId(p);rolePermissionMapper.insert(x);} }
}
