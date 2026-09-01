package com.erp.module.masterdata.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.module.masterdata.entity.Supplier;
import com.erp.module.masterdata.mapper.SupplierMapper;
import com.erp.module.system.service.DocSequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierMapper supplierMapper;
    private final DocSequenceService docSequenceService;

    public PageResult<Supplier> page(long page, long size, String keyword) {
        Page<Supplier> result = supplierMapper.selectPage(new Page<>(page, size),
                Wrappers.<Supplier>lambdaQuery()
                        .and(StringUtils.hasText(keyword),
                                w -> w.like(Supplier::getName, keyword)
                                        .or().like(Supplier::getCode, keyword)
                                        .or().like(Supplier::getContact, keyword)
                                        .or().like(Supplier::getPhone, keyword))
                        .orderByDesc(Supplier::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    @Transactional
    public Long create(Supplier supplier) {
        if (!StringUtils.hasText(supplier.getCode())) {
            // 供应商编码自动生成:SUP + 6 位序号
            supplier.setCode("SUP" + String.format("%06d", docSequenceService.next("SUP", "ALL")));
        }
        checkDuplicate(supplier, null);
        supplier.setId(null);
        if (!StringUtils.hasText(supplier.getSettleType())) {
            supplier.setSettleType("现结");
        }
        supplier.setIsActive(1);
        supplierMapper.insert(supplier);
        return supplier.getId();
    }

    @Transactional
    public void update(Supplier supplier) {
        Supplier db = requireById(supplier.getId());
        if (db.getIsActive() == 0) {
            throw new BusinessException("已停用档案不能修改,请先启用");
        }
        checkDuplicate(supplier, supplier.getId());
        // 编码生成后不变,状态走独立接口
        supplier.setCode(null);
        supplier.setIsActive(null);
        supplierMapper.updateById(supplier);
    }

    /** 停用/启用(档案不物理删除);停用不影响历史单据与应付 */
    @Transactional
    public void toggleStatus(Long id, boolean active) {
        requireById(id);
        Supplier p = new Supplier();
        p.setId(id);
        p.setIsActive(active ? 1 : 0);
        supplierMapper.updateById(p);
    }

    private Supplier requireById(Long id) {
        Supplier db = supplierMapper.selectById(id);
        if (db == null) {
            throw new BusinessException("供应商不存在");
        }
        return db;
    }

    /** 编码与名称全库唯一 */
    private void checkDuplicate(Supplier supplier, Long excludeId) {
        Long codeCount = supplierMapper.selectCount(Wrappers.<Supplier>lambdaQuery()
                .eq(Supplier::getCode, supplier.getCode())
                .ne(excludeId != null, Supplier::getId, excludeId));
        if (codeCount != null && codeCount > 0) {
            throw new BusinessException("供应商编码已存在:" + supplier.getCode());
        }
        Long nameCount = supplierMapper.selectCount(Wrappers.<Supplier>lambdaQuery()
                .eq(Supplier::getName, supplier.getName())
                .ne(excludeId != null, Supplier::getId, excludeId));
        if (nameCount != null && nameCount > 0) {
            throw new BusinessException("供应商名称已存在:" + supplier.getName());
        }
    }
}
