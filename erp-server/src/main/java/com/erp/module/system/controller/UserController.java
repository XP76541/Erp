package com.erp.module.system.controller;

import com.erp.common.Result;
import com.erp.module.system.entity.SysUser;
import com.erp.module.system.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户列表(客户档案的业务员下拉等场景使用);只暴露非敏感字段
 */
@RestController
@RequiredArgsConstructor
public class UserController {

    private final SysUserMapper sysUserMapper;

    public record UserOption(Long id, String username, String realName) {
    }

    @GetMapping("/users")
    public Result<List<UserOption>> listActive() {
        List<SysUser> users = sysUserMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getIsActive, 1)
                .orderByAsc(SysUser::getId));
        return Result.ok(users.stream()
                .map(u -> new UserOption(u.getId(), u.getUsername(), u.getRealName()))
                .toList());
    }
}
