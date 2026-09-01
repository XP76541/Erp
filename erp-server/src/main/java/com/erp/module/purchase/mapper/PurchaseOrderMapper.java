package com.erp.module.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.purchase.entity.PurchaseOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDate;
import java.util.List;

/**
 * 采购订单Mapper接口
 */
@Mapper
public interface PurchaseOrderMapper extends BaseMapper<PurchaseOrder> {

    /**
     * 查询指定供应商的采购订单列表
     */
    @Select("SELECT * FROM purchase_order WHERE supplier_id = #{supplierId} AND status != 'VOID' ORDER BY biz_date DESC, id DESC")
    List<PurchaseOrder> selectBySupplierId(@Param("supplierId") Long supplierId);

    /**
     * 查询指定仓库的采购订单列表
     */
    @Select("SELECT * FROM purchase_order WHERE warehouse_id = #{warehouseId} AND status != 'VOID' ORDER BY biz_date DESC, id DESC")
    List<PurchaseOrder> selectByWarehouseId(@Param("warehouseId") Long warehouseId);

    /**
     * 查询指定日期范围内的采购订单
     */
    @Select("SELECT * FROM purchase_order WHERE biz_date BETWEEN #{startDate} AND #{endDate} AND status != 'VOID' ORDER BY biz_date DESC, id DESC")
    List<PurchaseOrder> selectByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 查询指定状态的采购订单列表
     */
    @Select("SELECT * FROM purchase_order WHERE status = #{status} ORDER BY biz_date DESC, id DESC")
    List<PurchaseOrder> selectByStatus(@Param("status") String status);

    /**
     * 根据单号查询采购订单
     */
    @Select("SELECT * FROM purchase_order WHERE doc_no = #{docNo}")
    PurchaseOrder selectByDocNo(@Param("docNo") String docNo);

    /**
     * 查询待审核的采购订单数量
     */
    @Select("SELECT COUNT(*) FROM purchase_order WHERE status = 'DRAFT'")
    int countDraftOrders();

    /**
     * 查询已审核未入库的采购订单数量
     */
    @Select("SELECT COUNT(*) FROM purchase_order WHERE status = 'AUDITED' AND " +
            "id NOT IN (SELECT order_id FROM purchase_inbound)")
    int countUnreceivedOrders();

    /** 状态机抢占:DRAFT→AUDITED 原子迁移,返回 0 表示已被审或不存在 */
    @Update("UPDATE purchase_order SET status = 'AUDITED', audit_by = #{userId}, audit_at = SYSDATETIME() " +
            "WHERE id = #{id} AND status = 'DRAFT'")
    int claimAudit(@Param("id") Long id, @Param("userId") Long userId);

    /** 状态机抢占:DRAFT→VOID 原子迁移,返回 0 表示已被驳回或不存在 */
    @Update("UPDATE purchase_order SET status = 'VOID', reject_by = #{userId}, reject_at = SYSDATETIME() " +
            "WHERE id = #{id} AND status = 'DRAFT'")
    int claimReject(@Param("id") Long id, @Param("userId") Long userId);
}