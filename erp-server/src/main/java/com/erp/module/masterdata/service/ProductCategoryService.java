package com.erp.module.masterdata.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.common.BusinessException;
import com.erp.module.masterdata.entity.ProductCategory;
import com.erp.module.masterdata.mapper.ProductCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductCategoryService {

    private final ProductCategoryMapper categoryMapper;

    /** 全量列表(分类数量少,不分页),按排序号、id 升序 */
    public List<ProductCategory> listAll() {
        return categoryMapper.selectList(Wrappers.<ProductCategory>lambdaQuery()
                .orderByAsc(ProductCategory::getSort)
                .orderByAsc(ProductCategory::getId));
    }

    @Transactional
    public Long create(ProductCategory category) {
        category.setId(null);
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        validateParent(category.getParentId(), null);
        checkDuplicate(category.getName(), category.getParentId(), null);
        category.setSort(category.getSort() == null ? 0 : category.getSort());
        category.setIsActive(1);
        categoryMapper.insert(category);
        return category.getId();
    }

    @Transactional
    public void update(ProductCategory category) {
        ProductCategory db = requireById(category.getId());
        if (db.getIsActive() == 0) {
            throw new BusinessException("已停用分类不能修改,请先启用");
        }
        if (category.getParentId() == null) {
            category.setParentId(db.getParentId());
        }
        validateParent(category.getParentId(), category.getId());
        checkDuplicate(category.getName(), category.getParentId(), category.getId());
        // 编码式约束:状态不在本接口修改,走独立接口
        category.setIsActive(null);
        categoryMapper.updateById(category);
    }

    /** 停用/启用(档案不物理删除);有启用中的子分类时不允许停用 */
    @Transactional
    public void toggleStatus(Long id, boolean active) {
        requireById(id);
        if (!active) {
            Long children = categoryMapper.selectCount(Wrappers.<ProductCategory>lambdaQuery()
                    .eq(ProductCategory::getParentId, id)
                    .eq(ProductCategory::getIsActive, 1));
            if (children != null && children > 0) {
                throw new BusinessException("该分类下还有启用中的子分类,请先停用子分类");
            }
        }
        ProductCategory p = new ProductCategory();
        p.setId(id);
        p.setIsActive(active ? 1 : 0);
        categoryMapper.updateById(p);
    }

    private ProductCategory requireById(Long id) {
        ProductCategory db = categoryMapper.selectById(id);
        if (db == null) {
            throw new BusinessException("分类不存在");
        }
        return db;
    }

    /** 仅支持两级:parentId=0 为根;否则上级必须存在且本身是根分类(该约束同时杜绝了循环引用) */
    private void validateParent(Long parentId, Long selfId) {
        if (parentId == 0) {
            return;
        }
        if (parentId.equals(selfId)) {
            throw new BusinessException("上级分类不能选择自己");
        }
        ProductCategory parent = categoryMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException("上级分类不存在");
        }
        if (parent.getParentId() != 0) {
            throw new BusinessException("仅支持两级分类,上级分类必须是根分类");
        }
    }

    /** 同一上级下分类名称唯一 */
    private void checkDuplicate(String name, Long parentId, Long excludeId) {
        Long count = categoryMapper.selectCount(Wrappers.<ProductCategory>lambdaQuery()
                .eq(ProductCategory::getName, name)
                .eq(ProductCategory::getParentId, parentId)
                .ne(excludeId != null, ProductCategory::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException("同一上级下已存在同名分类:" + name);
        }
    }
}
