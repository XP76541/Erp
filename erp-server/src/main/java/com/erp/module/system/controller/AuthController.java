package com.erp.module.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.BusinessException;
import com.erp.common.Result;
import com.erp.module.system.AuthInterceptor;
import com.erp.module.system.TokenStore;
import com.erp.module.system.entity.SysUser;
import com.erp.module.system.mapper.SysUserMapper;
import com.erp.module.system.service.SystemAuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * 认证接口(骨架实现,菜单/按钮权限点后续接入 sys_permission)
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenStore tokenStore;
    private final SystemAuthorizationService authorizationService;

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record LoginResponse(String token, String username, String realName, Set<String> roleCodes) {
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, req.username()));
        if (user == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getIsActive() == null || user.getIsActive() == 0) {
            throw new BusinessException(403, "账号已被禁用,请联系管理员");
        }
        TokenStore.LoginUser loginUser =
                new TokenStore.LoginUser(user.getId(), user.getUsername(), user.getRealName());
        String token = tokenStore.create(loginUser);
        return Result.ok(new LoginResponse(token, user.getUsername(), user.getRealName(),
                authorizationService.roleCodes(loginUser)));
    }

    @GetMapping("/me")
    public Result<AuthUserResponse> me(
            @RequestAttribute(AuthInterceptor.ATTR_LOGIN_USER) TokenStore.LoginUser user) {
        return Result.ok(new AuthUserResponse(user.userId(), user.username(), user.realName(),
                authorizationService.roleCodes(user)));
    }

    public record AuthUserResponse(Long userId, String username, String realName, Set<String> roleCodes) {
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            tokenStore.remove(authorization.substring("Bearer ".length()));
        }
        return Result.ok();
    }
}
