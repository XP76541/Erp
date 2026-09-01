package com.erp.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.inventory.entity.Inventory;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface InventoryMapper extends BaseMapper<Inventory> {

    /**
     * 行锁读取结存(事务内使用):并发入库/出库串行化,防止加权平均成本算错
     */
    @Select("SELECT * FROM inventory WITH (UPDLOCK, ROWLOCK) "
            + "WHERE product_id = #{productId} AND warehouse_id = #{warehouseId}")
    Inventory selectForUpdate(@Param("productId") Long productId, @Param("warehouseId") Long warehouseId);
}
