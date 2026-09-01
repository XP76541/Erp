package com.erp.module.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.purchase.entity.PurchaseInbound;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

public interface PurchaseInboundMapper extends BaseMapper<PurchaseInbound> {

    /** 状态机抢占:DRAFT→AUDITED 原子迁移,返回 0 表示已被审或不存在 */
    @Update("UPDATE purchase_inbound SET status = 'AUDITED', audit_by = #{userId}, audit_at = SYSDATETIME() "
            + "WHERE id = #{id} AND status = 'DRAFT'")
    int claimAudit(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 根据采购订单ID查询关联的入库单
     */
    @Select("SELECT * FROM purchase_inbound WHERE doc_id = #{orderId} AND doc_type = 'PURCHASE_ORDER' ORDER BY created_at")
    List<PurchaseInbound> selectByOrderId(@Param("orderId") Long orderId);

    /**
     * 检查指定采购订单是否已有入库单
     */
    @Select("SELECT COUNT(*) > 0 FROM purchase_inbound WHERE doc_id = #{orderId} AND doc_type = 'PURCHASE_ORDER'")
    boolean existsByOrderId(@Param("orderId") Long orderId);

    /**
     * 根据供应商ID查询入库单列表
     */
    @Select("SELECT * FROM purchase_inbound WHERE supplier_id = #{supplierId} ORDER BY biz_date DESC, id DESC")
    List<PurchaseInbound> selectBySupplierId(@Param("supplierId") Long supplierId);

    /**
     * 查询指定日期范围内的入库单
     */
    @Select("SELECT * FROM purchase_inbound WHERE biz_date BETWEEN #{startDate} AND #{endDate} ORDER BY biz_date DESC, id DESC")
    List<PurchaseInbound> selectByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate);
}
