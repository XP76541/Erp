package com.erp.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.inventory.entity.InventoryTransferItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

/**
 * 库存调拨明细Mapper
 */
public interface InventoryTransferItemMapper extends BaseMapper<InventoryTransferItem> {

    /**
     * 根据调拨单ID查询明细列表
     */
    @Select("SELECT * FROM inventory_transfer_item WHERE transfer_id = #{transferId} ORDER BY line_no")
    List<InventoryTransferItem> selectByTransferId(@Param("transferId") Long transferId);

    /**
     * 批量更新调拨明细的仓库信息
     */
    @Update("<script>" +
            "UPDATE inventory_transfer_item SET to_warehouse_id = #{toWarehouseId}, updated_at = SYSDATETIME() " +
            "WHERE transfer_id = #{transferId} AND product_id IN " +
            "<foreach collection='productIds' item='productId' open='(' separator=',' close=')'>" +
            "#{productId} " +
            "</foreach>" +
            "</script>")
    int batchUpdateToWarehouse(@Param("transferId") Long transferId,
                              @Param("toWarehouseId") Long toWarehouseId,
                              @Param("productIds") List<Long> productIds);
}