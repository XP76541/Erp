package com.erp.module.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.sales.entity.SalesOutboundItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.math.BigDecimal;
import java.util.List;

/**
 * 销售出库单明细Mapper接口
 */
@Mapper
public interface SalesOutboundItemMapper extends BaseMapper<SalesOutboundItem> {

    /**
     * 根据出库单ID查询明细列表
     */
    @Select("SELECT * FROM sales_outbound_item WHERE outbound_id = #{outboundId} ORDER BY line_no")
    List<SalesOutboundItem> selectByOutboundId(@Param("outboundId") Long outboundId);

    /**
     * 根据订单明细ID查询出库明细
     */
    @Select("SELECT * FROM sales_outbound_item WHERE order_item_id = #{orderItemId}")
    List<SalesOutboundItem> selectByOrderItemId(@Param("orderItemId") Long orderItemId);

    /**
     * 查询指定订单已发货的数量
     */
    @Select("SELECT SUM(qty) FROM sales_outbound_item WHERE order_item_id = #{orderItemId}")
    BigDecimal selectShippedQtyByOrderItemId(@Param("orderItemId") Long orderItemId);

    /**
     * 统计指定订单的发货情况
     */
    @Select("SELECT soi.order_item_id, soi.qty as shipped_qty, soi2.qty as ordered_qty, " +
            "(soi2.qty - COALESCE(soi.qty, 0)) as unshipped_qty " +
            "FROM sales_outbound_item soi " +
            "RIGHT JOIN sales_order_item soi2 ON soi.order_item_id = soi2.id " +
            "WHERE soi2.order_id = #{orderId} " +
            "ORDER BY soi2.line_no")
    List<Object[]> selectShippingStatsByOrderId(@Param("orderId") Long orderId);
}