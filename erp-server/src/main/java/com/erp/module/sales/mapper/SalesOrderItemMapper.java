package com.erp.module.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.sales.entity.SalesOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.math.BigDecimal;
import java.util.List;

/**
 * 销售订单明细Mapper接口
 */
@Mapper
public interface SalesOrderItemMapper extends BaseMapper<SalesOrderItem> {

    /**
     * 根据订单ID查询明细列表
     */
    @Select("SELECT * FROM sales_order_item WHERE order_id = #{orderId} ORDER BY line_no")
    List<SalesOrderItem> selectByOrderId(@Param("orderId") Long orderId);

    /**
     * 根据订单ID和产品ID查询明细
     */
    @Select("SELECT * FROM sales_order_item WHERE order_id = #{orderId} AND product_id = #{productId}")
    SalesOrderItem selectByOrderIdAndProductId(@Param("orderId") Long orderId, @Param("productId") Long productId);

    /**
     * 查询指定产品的销售订单明细（用于库存需求分析）
     */
    @Select("SELECT soi.*, so.doc_no, so.customer_id, so.biz_date " +
            "FROM sales_order_item soi " +
            "JOIN sales_order so ON soi.order_id = so.id " +
            "WHERE soi.product_id = #{productId} AND so.status = 'AUDITED' AND so.ship_status != 'SHIPPED' " +
            "ORDER BY so.biz_date, soi.line_no")
    List<SalesOrderItem> selectUnshippedByProductId(@Param("productId") Long productId);

    /**
     * 查询指定仓库的待发货数量汇总
     */
    @Select("SELECT soi.product_id, SUM(soi.qty - soi.shipped_qty) as unshipped_qty " +
            "FROM sales_order_item soi " +
            "JOIN sales_order so ON soi.order_id = so.id " +
            "JOIN sales_outbound_item soi2 ON soi.id = soi2.order_item_id " +
            "WHERE so.status = 'AUDITED' AND so.warehouse_id = #{warehouseId} " +
            "AND soi2.id IS NULL " +
            "GROUP BY soi.product_id")
    List<Object[]> selectUnshippedQtyByWarehouse(@Param("warehouseId") Long warehouseId);

    /**
     * 更新发货数量
     */
    @Update("UPDATE sales_order_item SET shipped_qty = shipped_qty + #{shippedQty} " +
            "WHERE id = #{orderItemId} AND shipped_qty + #{shippedQty} <= qty")
    int updateShippedQty(@Param("orderItemId") Long orderItemId, @Param("shippedQty") BigDecimal shippedQty);

    /**
     * 查询指定订单明细已发货的数量
     */
    @Select("SELECT COALESCE(SUM(qty), 0) FROM sales_outbound_item WHERE order_item_id = #{orderItemId}")
    BigDecimal selectShippedQtyByOrderItemId(@Param("orderItemId") Long orderItemId);
}