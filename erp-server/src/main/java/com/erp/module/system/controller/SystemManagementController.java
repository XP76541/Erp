package com.erp.module.system.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.module.system.AuthInterceptor;
import com.erp.module.system.TokenStore;
import com.erp.module.system.entity.OperationLog;
import com.erp.module.system.entity.SysPermission;
import com.erp.module.system.entity.SysRole;
import com.erp.module.system.service.OperationLogService;
import com.erp.module.system.service.SystemAuthorizationService;
import com.erp.module.system.service.SystemManagementService;
import com.erp.module.system.mapper.OperationLogMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
public class SystemManagementController {
    private final SystemManagementService service;
    private final SystemAuthorizationService authorization;
    private final OperationLogMapper logMapper;
    private final OperationLogService logService;

    public record UserRequest(@NotBlank String username, @NotBlank String realName, String password, Integer active, List<Long> roleIds) {}
    public record UserUpdateRequest(@NotBlank String realName, Integer active, List<Long> roleIds) {}
    public record PasswordRequest(@NotBlank String password) {}
    public record RoleRequest(@NotBlank String name, String remark, List<Long> permissionIds) {}

    private void admin(TokenStore.LoginUser user) { authorization.requireAdmin(user); }
    private TokenStore.LoginUser current(jakarta.servlet.http.HttpServletRequest r) { return (TokenStore.LoginUser) r.getAttribute(AuthInterceptor.ATTR_LOGIN_USER); }

    @GetMapping("/users") public Result<PageResult<SystemManagementService.UserView>> users(@RequestParam(defaultValue="1") long page, @RequestParam(defaultValue="10") long size, @RequestParam(required=false) String keyword, @RequestParam(required=false) Integer active, jakarta.servlet.http.HttpServletRequest r) { admin(current(r)); return Result.ok(service.users(page,size,keyword,active)); }
    @PostMapping("/users") public Result<Long> create(@Valid @RequestBody UserRequest x, jakarta.servlet.http.HttpServletRequest r) { var u=current(r); admin(u); if(x.password()==null||x.password().isBlank()) throw new com.erp.common.BusinessException("请输入初始密码"); Long id=service.createUser(x.username(),x.realName(),x.password(),x.active(),x.roleIds()); logService.record(u,"system_user","CREATE","SYS_USER",id,null,"创建用户",r.getRemoteAddr()); return Result.ok(id); }
    @PutMapping("/users/{id}") public Result<Void> update(@PathVariable Long id,@Valid @RequestBody UserUpdateRequest x,jakarta.servlet.http.HttpServletRequest r){var u=current(r);admin(u);service.updateUser(id,x.realName(),x.active(),x.roleIds());logService.record(u,"system_user","UPDATE","SYS_USER",id,null,"更新用户",r.getRemoteAddr());return Result.ok();}
    @PutMapping("/users/{id}/password") public Result<Void> password(@PathVariable Long id,@Valid @RequestBody PasswordRequest x,jakarta.servlet.http.HttpServletRequest r){var u=current(r);admin(u);service.resetPassword(id,x.password());logService.record(u,"system_user","RESET_PASSWORD","SYS_USER",id,null,"重置密码",r.getRemoteAddr());return Result.ok();}

    @GetMapping("/roles") public Result<List<SystemManagementService.RoleView>> roles(jakarta.servlet.http.HttpServletRequest r){admin(current(r));return Result.ok(service.roles());}
    @GetMapping("/permissions") public Result<List<SysPermission>> permissions(jakarta.servlet.http.HttpServletRequest r){admin(current(r));return Result.ok(service.permissions());}
    @PutMapping("/roles/{id}") public Result<Void> role(@PathVariable Long id,@Valid @RequestBody RoleRequest x,jakarta.servlet.http.HttpServletRequest r){var u=current(r);admin(u);service.updateRole(id,x.name(),x.remark(),x.permissionIds());logService.record(u,"system_role","UPDATE_PERMISSION","SYS_ROLE",id,null,"更新角色权限",r.getRemoteAddr());return Result.ok();}

    @GetMapping("/operation-logs") public Result<PageResult<OperationLog>> logs(@RequestParam(defaultValue="1") long page,@RequestParam(defaultValue="20") long size,@RequestParam(required=false) String keyword,@RequestParam(required=false) String action,@RequestParam(required=false) String module,@RequestParam(required=false) String from,@RequestParam(required=false) String to,jakarta.servlet.http.HttpServletRequest r){admin(current(r));var q=Wrappers.<OperationLog>lambdaQuery().eq(action!=null&&!action.isBlank(),OperationLog::getAction,action).eq(module!=null&&!module.isBlank(),OperationLog::getModule,module).and(keyword!=null&&!keyword.isBlank(),w->w.like(OperationLog::getUserName,keyword).or().like(OperationLog::getDocNo,keyword));if(from!=null&&!from.isBlank())q.ge(OperationLog::getCreatedAt,LocalDate.parse(from).atStartOfDay());if(to!=null&&!to.isBlank())q.lt(OperationLog::getCreatedAt,LocalDate.parse(to).plusDays(1).atStartOfDay());q.orderByDesc(OperationLog::getCreatedAt);Page<OperationLog> p=logMapper.selectPage(new Page<>(page,size),q);return Result.ok(PageResult.of(p.getTotal(),p.getRecords()));}
}
