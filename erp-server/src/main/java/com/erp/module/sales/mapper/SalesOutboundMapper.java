package com.erp.module.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.sales.entity.SalesOutbound;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDate;
import java.util.List;

/**
 * 销售出库单Mapper接口
 */
@Mapper
public interface SalesOutboundMapper extends BaseMapper<SalesOutbound> {

    /**
     * 查询指定订单的出库单列表
     */
    @Select("SELECT * FROM sales_outbound WHERE order_id = #{orderId} ORDER BY biz_date DESC, id DESC")
    List<SalesOutbound> selectByOrderId(@Param("orderId") Long orderId);

    /**
     * 查询指定客户的出库单列表
     */
    @Select("SELECT * FROM sales_outbound WHERE customer_id = #{customerId} ORDER BY biz_date DESC, id DESC")
    List<SalesOutbound> selectByCustomerId(@Param("customerId") Long customerId);

    /**
     * 查询指定仓库的出库单列表
     */
    @Select("SELECT * FROM sales_outbound WHERE warehouse_id = #{warehouseId} ORDER BY biz_date DESC, id DESC")
    List<SalesOutbound> selectByWarehouseId(@Param("warehouseId") Long warehouseId);

    /**
     * 查询指定日期范围内的出库单
     */
    @Select("SELECT * FROM sales_outbound WHERE biz_date BETWEEN #{startDate} AND #{endDate} ORDER BY biz_date DESC, id DESC")
    List<SalesOutbound> selectByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 查询指定状态的出库单列表
     */
    @Select("SELECT * FROM sales_outbound WHERE status = #{status} ORDER BY biz_date DESC, id DESC")
    List<SalesOutbound> selectByStatus(@Param("status") String status);

    /**
     * 根据单号查询出库单
     */
    @Select("SELECT * FROM sales_outbound WHERE doc_no = #{docNo}")
    SalesOutbound selectByDocNo(@Param("docNo") String docNo);

    /**
     * 查询待审核的出库单数量
     */
    @Select("SELECT COUNT(*) FROM sales_outbound WHERE status = 'DRAFT'")
    int countDraftOutbounds();

    /**
     * 查询已审核未收款的数量
     */
    @Select("SELECT COUNT(*) FROM sales_outbound WHERE status = 'AUDITED' AND " +
            "id NOT IN (SELECT outbound_id FROM receipt_allocation)")
    int countUnpaidOutbounds();

    /** 状态机抢占:DRAFT→AUDITED 原子迁移,返回 0 表示已被审或不存在 */
    @Update("UPDATE sales_outbound SET status = 'AUDITED', audit_by = #{userId}, audit_at = SYSDATETIME() " +
            "WHERE id = #{id} AND status = 'DRAFT'")
    int claimAudit(@Param("id") Long id, @Param("userId") Long userId);

    /** 状态机抢占:DRAFT→VOID 原子迁移,返回 0 表示已被驳回或不存在 */
    @Update("UPDATE sales_outbound SET status = 'VOID', reject_by = #{userId}, reject_at = SYSDATETIME() " +
            "WHERE id = #{id} AND status = 'DRAFT'")
    int claimReject(@Param("id") Long id, @Param("userId") Long userId);
}