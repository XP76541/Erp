package com.erp.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.inventory.entity.InventoryCheckItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

/**
 * 库存盘点明细Mapper
 */
public interface InventoryCheckItemMapper extends BaseMapper<InventoryCheckItem> {

    /**
     * 根据盘点单ID查询明细列表
     */
    @Select("SELECT * FROM inventory_check_item WHERE check_id = #{checkId} ORDER BY product_id")
    List<InventoryCheckItem> selectByCheckId(@Param("checkId") Long checkId);

    /**
     * 批量更新盘点明细的实际数量
     */
    @Update("<script>" +
            "UPDATE inventory_check_item SET " +
            "actual_qty = CASE " +
            "<foreach collection='items' item='item'>" +
            "WHEN product_id = #{item.productId} THEN #{item.actualQty} " +
            "</foreach>" +
            ", diff_qty = CASE " +
            "<foreach collection='items' item='item'>" +
            "WHEN product_id = #{item.productId} THEN (SELECT qty FROM inventory WHERE product_id = #{item.productId} AND warehouse_id = #{warehouseId}) - #{item.actualQty} " +
            "</foreach>" +
            ", amount = CASE " +
            "<foreach collection='items' item='item'>" +
            "WHEN product_id = #{item.productId} THEN (SELECT price FROM product WHERE id = #{item.productId}) * #{item.actualQty} " +
            "</foreach>" +
            ", status = CASE " +
            "<foreach collection='items' item='item'>" +
            "WHEN product_id = #{item.productId} THEN " +
            "CASE WHEN (SELECT qty FROM inventory WHERE product_id = #{item.productId} AND warehouse_id = #{warehouseId}) = #{item.actualQty} THEN 'NORMAL' " +
            "WHEN (SELECT qty FROM inventory WHERE product_id = #{item.productId} AND warehouse_id = #{warehouseId}) > #{item.actualQty} THEN 'MISSING' " +
            "ELSE 'EXCESS' END " +
            "</foreach>" +
            ", updated_at = SYSDATETIME() " +
            "WHERE check_id = #{checkId} AND product_id IN " +
            "<foreach collection='items' item='item' open='(' separator=',' close=')'>" +
            "#{item.productId} " +
            "</foreach>" +
            "</script>")
    int batchUpdateActualQty(@Param("checkId") Long checkId,
                            @Param("warehouseId") Long warehouseId,
                            @Param("items") List<ActualQtyUpdate> items);

    /**
     * 更新盘点单的统计数据
     */
    @Update("UPDATE inventory_check c SET " +
            "c.total_items = (SELECT COUNT(*) FROM inventory_check_item i WHERE i.check_id = c.id), " +
            "c.total_amount = (SELECT COALESCE(SUM(i.amount), 0) FROM inventory_check_item i WHERE i.check_id = c.id), " +
            "c.diff_items = (SELECT COUNT(*) FROM inventory_check_item i WHERE i.check_id = c.id AND i.status <> 'NORMAL'), " +
            "c.diff_amount = (SELECT COALESCE(SUM(CASE WHEN i.status <> 'NORMAL' THEN i.amount ELSE 0 END), 0) " +
            "FROM inventory_check_item i WHERE i.check_id = c.id) " +
            "WHERE c.id = #{checkId}")
    int updateCheckStats(@Param("checkId") Long checkId);

    // 用于批量更新的内部类
    class ActualQtyUpdate {
        private Long productId;
        private java.math.BigDecimal actualQty;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public java.math.BigDecimal getActualQty() { return actualQty; }
        public void setActualQty(java.math.BigDecimal actualQty) { this.actualQty = actualQty; }
    }
}