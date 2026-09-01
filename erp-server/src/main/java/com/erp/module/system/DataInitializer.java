package com.erp.module.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.module.masterdata.entity.Warehouse;
import com.erp.module.masterdata.mapper.WarehouseMapper;
import com.erp.module.system.entity.SysRole;
import com.erp.module.system.entity.SysUser;
import com.erp.module.system.entity.SysUserRole;
import com.erp.module.system.mapper.SysRoleMapper;
import com.erp.module.system.mapper.SysUserMapper;
import com.erp.module.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 首次启动初始化:默认管理员与仓库(角色已由 V1__init.sql 灌入)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final WarehouseMapper warehouseMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initAdmin();
        initWarehouses();
    }

    private void initAdmin() {
        if (sysUserMapper.selectCount(null) > 0) {
            return;
        }
        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRealName("管理员");
        admin.setIsActive(1);
        sysUserMapper.insert(admin);

        SysRole adminRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, "ADMIN"));
        if (adminRole != null) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(admin.getId());
            userRole.setRoleId(adminRole.getId());
            sysUserRoleMapper.insert(userRole);
        }
        log.info("已初始化默认账号:admin / admin123");
    }

    private void initWarehouses() {
        if (warehouseMapper.selectCount(null) > 0) {
            return;
        }
        insertWarehouse("WH01", "正品仓", "正品仓");
        insertWarehouse("WH02", "次品仓", "次品仓");
        insertWarehouse("WH03", "样品仓", "样品仓");
        log.info("已初始化默认仓库:WH01/WH02/WH03");
    }

    private void insertWarehouse(String code, String name, String type) {
        Warehouse w = new Warehouse();
        w.setCode(code);
        w.setName(name);
        w.setType(type);
        w.setIsActive(1);
        warehouseMapper.insert(w);
    }
}
