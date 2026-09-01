package com.erp.module.masterdata.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.common.BusinessException;
import com.erp.module.masterdata.entity.Warehouse;
import com.erp.module.masterdata.mapper.WarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    /** 允许的仓库类型(与表注释一致) */
    private static final Set<String> ALLOWED_TYPES = Set.of("正品仓", "次品仓", "样品仓");

    private final WarehouseMapper warehouseMapper;

    /** 全量列表(仓库数量少,不分页),按编码升序 */
    public List<Warehouse> listAll() {
        return warehouseMapper.selectList(Wrappers.<Warehouse>lambdaQuery()
                .orderByAsc(Warehouse::getCode));
    }

    @Transactional
    public Long create(Warehouse warehouse) {
        warehouse.setId(null);
        checkType(warehouse.getType());
        checkCodeDuplicate(warehouse.getCode(), null);
        warehouse.setIsActive(1);
        warehouseMapper.insert(warehouse);
        return warehouse.getId();
    }

    @Transactional
    public void update(Warehouse warehouse) {
        Warehouse db = requireById(warehouse.getId());
        if (db.getIsActive() == 0) {
            throw new BusinessException("已停用档案不能修改,请先启用");
        }
        checkType(warehouse.getType());
        // 编码创建后不可修改,状态走独立接口
        warehouse.setCode(null);
        warehouse.setIsActive(null);
        warehouseMapper.updateById(warehouse);
    }

    /** 停用/启用(档案不物理删除) */
    @Transactional
    public void toggleStatus(Long id, boolean active) {
        requireById(id);
        Warehouse p = new Warehouse();
        p.setId(id);
        p.setIsActive(active ? 1 : 0);
        warehouseMapper.updateById(p);
    }

    private Warehouse requireById(Long id) {
        Warehouse db = warehouseMapper.selectById(id);
        if (db == null) {
            throw new BusinessException("仓库不存在");
        }
        return db;
    }

    private void checkType(String type) {
        if (type == null || !ALLOWED_TYPES.contains(type)) {
            throw new BusinessException("仓库类型必须是:正品仓 / 次品仓 / 样品仓");
        }
    }

    private void checkCodeDuplicate(String code, Long excludeId) {
        Long count = warehouseMapper.selectCount(Wrappers.<Warehouse>lambdaQuery()
                .eq(Warehouse::getCode, code)
                .ne(excludeId != null, Warehouse::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException("仓库编码已存在:" + code);
        }
    }
}
