package com.erp.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.inventory.entity.InventoryLedger;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

public interface InventoryLedgerMapper extends BaseMapper<InventoryLedger> {

    /** 查询指定仓库截至日期每个商品最后一条流水快照。没有流水的商品不会返回。 */
    @Select("""
            SELECT l.*
            FROM inventory_ledger l
            WHERE l.warehouse_id = #{warehouseId}
              AND l.biz_date <= #{date}
              AND NOT EXISTS (
                  SELECT 1
                  FROM inventory_ledger newer
                  WHERE newer.warehouse_id = l.warehouse_id
                    AND newer.product_id = l.product_id
                    AND newer.biz_date <= #{date}
                    AND (
                        newer.biz_date > l.biz_date
                        OR (newer.biz_date = l.biz_date AND newer.created_at > l.created_at)
                        OR (newer.biz_date = l.biz_date AND newer.created_at = l.created_at AND newer.id > l.id)
                    )
              )
            ORDER BY l.product_id
            """)
    List<InventoryLedger> selectLatestByWarehouseAsOf(@Param("warehouseId") Long warehouseId,
                                                       @Param("date") LocalDate date);
}
