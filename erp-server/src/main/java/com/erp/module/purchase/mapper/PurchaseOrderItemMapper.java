package com.erp.module.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.purchase.entity.PurchaseOrderItem;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.math.BigDecimal;
import java.util.List;

/**
 * 采购订单明细Mapper接口
 */
@Mapper
public interface PurchaseOrderItemMapper extends BaseMapper<PurchaseOrderItem> {

    /**
     * 根据采购订单ID查询明细列表
     */
    @Select("SELECT * FROM purchase_order_item WHERE order_id = #{orderId} ORDER BY line_no")
    List<PurchaseOrderItem> selectByOrderId(@Param("orderId") Long orderId);

    /**
     * 根据商品ID查询采购订单明细
     */
    @Select("SELECT * FROM purchase_order_item WHERE product_id = #{productId} ORDER BY created_at DESC")
    List<PurchaseOrderItem> selectByProductId(@Param("productId") Long productId);

    /**
     * 查询指定采购订单已入库的数量
     */
    @Select("SELECT COALESCE(SUM(received_qty), 0) FROM purchase_order_item WHERE order_id = #{orderId}")
    BigDecimal selectReceivedQtyByOrderId(@Param("orderId") Long orderId);

    /**
     * 更新已入库数量
     */
    @Update("UPDATE purchase_order_item SET received_qty = received_qty + #{qty}, updated_at = SYSDATETIME() " +
            "WHERE id = #{itemId} AND received_qty < qty")
    int updateReceivedQty(@Param("itemId") Long itemId, @Param("qty") BigDecimal qty);

    /**
     * 批量更新已入库数量
     */
    @Update("<script>" +
            "UPDATE purchase_order_item SET received_qty = CASE " +
            "<foreach collection='items' item='item'>" +
            "WHEN id = #{item.itemId} THEN received_qty + #{item.qty} " +
            "</foreach>" +
            ", updated_at = SYSDATETIME() " +
            "WHERE order_id = #{orderId} AND id IN " +
            "<foreach collection='items' item='item' open='(' separator=',' close=')'>" +
            "#{item.itemId} " +
            "</foreach>" +
            "</script>")
    int batchUpdateReceivedQty(@Param("orderId") Long orderId, @Param("items") List<ReceivedQtyUpdate> items);

    /**
     * 统计指定订单的入库情况
     */
    @Select("SELECT poi.id, poi.line_no, poi.qty as ordered_qty, poi.received_qty, " +
            "(poi.qty - COALESCE(poi.received_qty, 0)) as remaining_qty, " +
            "poi.product_id, p.name as product_name " +
            "FROM purchase_order_item poi " +
            "LEFT JOIN product p ON poi.product_id = p.id " +
            "WHERE poi.order_id = #{orderId} " +
            "ORDER BY poi.line_no")
    List<OrderItemStats> selectOrderItemStats(@Param("orderId") Long orderId);

    /**
     * 查询采购订单总金额
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM purchase_order_item WHERE order_id = #{orderId}")
    BigDecimal selectTotalAmountByOrderId(@Param("orderId") Long orderId);

    /**
     * 更新订单明细金额
     */
    @Update("UPDATE purchase_order_item SET amount = qty * price, updated_at = SYSDATETIME() " +
            "WHERE id = #{itemId}")
    int updateItemAmount(@Param("itemId") Long itemId);

    @Data
    static class OrderItemStats {
        private Long id;
        private Integer lineNo;
        private BigDecimal orderedQty;
        private BigDecimal receivedQty;
        private BigDecimal remainingQty;
        private Long productId;
        private String productName;
    }

    @Data
    static class ReceivedQtyUpdate {
        private Long itemId;
        private BigDecimal qty;
    }
}