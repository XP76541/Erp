package com.erp.module.masterdata.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.module.masterdata.entity.Product;
import com.erp.module.masterdata.mapper.ProductMapper;
import com.erp.module.system.service.DocSequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final DocSequenceService docSequenceService;

    public PageResult<Product> page(long page, long size, String keyword) {
        Page<Product> result = productMapper.selectPage(new Page<>(page, size),
                Wrappers.<Product>lambdaQuery()
                        .and(StringUtils.hasText(keyword),
                                w -> w.like(Product::getName, keyword)
                                        .or().like(Product::getCode, keyword)
                                        .or().like(Product::getSpec, keyword))
                        .orderByDesc(Product::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    @Transactional
    public Long create(Product product) {
        if (!StringUtils.hasText(product.getCode())) {
            // 商品编码自动生成:SKU + 6 位序号
            product.setCode("SKU" + String.format("%06d", docSequenceService.next("SKU", "ALL")));
        }
        checkDuplicate(product, null);
        product.setId(null);
        product.setIsActive(1);
        productMapper.insert(product);
        return product.getId();
    }

    @Transactional
    public void update(Product product) {
        Product db = requireById(product.getId());
        if (db.getIsActive() == 0) {
            throw new BusinessException("已停用档案不能修改,请先启用");
        }
        checkDuplicate(product, product.getId());
        // 编码与状态不在本接口修改:编码生成后不变,状态走独立接口
        product.setCode(null);
        product.setIsActive(null);
        productMapper.updateById(product);
    }

    /** 停用/启用(档案不物理删除) */
    @Transactional
    public void toggleStatus(Long id, boolean active) {
        requireById(id);
        Product p = new Product();
        p.setId(id);
        p.setIsActive(active ? 1 : 0);
        productMapper.updateById(p);
    }

    private Product requireById(Long id) {
        Product db = productMapper.selectById(id);
        if (db == null) {
            throw new BusinessException("商品不存在");
        }
        return db;
    }

    /** 同名同规格视为重复档案 */
    private void checkDuplicate(Product product, Long excludeId) {
        Long count = productMapper.selectCount(Wrappers.<Product>lambdaQuery()
                .eq(Product::getName, product.getName())
                .eq(Product::getSpec, product.getSpec() == null ? "" : product.getSpec())
                .ne(excludeId != null, Product::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException("已存在同名同规格的商品:" + product.getName());
        }
    }
}
