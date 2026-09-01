package com.erp.module.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.sales.entity.SalesOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDate;
import java.util.List;

/**
 * 销售订单Mapper接口
 */
@Mapper
public interface SalesOrderMapper extends BaseMapper<SalesOrder> {

    /**
     * 查询指定客户的销售订单列表
     */
    @Select("SELECT * FROM sales_order WHERE customer_id = #{customerId} AND status != 'VOID' ORDER BY biz_date DESC, id DESC")
    List<SalesOrder> selectByCustomerId(@Param("customerId") Long customerId);

    /**
     * 查询指定销售人员的销售订单列表
     */
    @Select("SELECT * FROM sales_order WHERE salesperson_id = #{salespersonId} AND status != 'VOID' ORDER BY biz_date DESC, id DESC")
    List<SalesOrder> selectBySalespersonId(@Param("salespersonId") Long salespersonId);

    /**
     * 查询指定日期范围内的销售订单
     */
    @Select("SELECT * FROM sales_order WHERE biz_date BETWEEN #{startDate} AND #{endDate} AND status != 'VOID' ORDER BY biz_date DESC, id DESC")
    List<SalesOrder> selectByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 查询指定状态的销售订单列表
     */
    @Select("SELECT * FROM sales_order WHERE status = #{status} ORDER BY biz_date DESC, id DESC")
    List<SalesOrder> selectByStatus(@Param("status") String status);

    /**
     * 根据单号查询销售订单
     */
    @Select("SELECT * FROM sales_order WHERE doc_no = #{docNo}")
    SalesOrder selectByDocNo(@Param("docNo") String docNo);

    /**
     * 查询待审核的销售订单数量
     */
    @Select("SELECT COUNT(*) FROM sales_order WHERE status = 'DRAFT'")
    int countDraftOrders();

    /**
     * 查询已审核未发货的销售订单数量
     */
    @Select("SELECT COUNT(*) FROM sales_order WHERE status = 'AUDITED' AND ship_status = 'UN_SHIPPED'")
    int countUnshippedOrders();

    /** 状态机抢占:DRAFT→AUDITED 原子迁移,返回 0 表示已被审或不存在 */
    @Update("UPDATE sales_order SET status = 'AUDITED', audit_by = #{userId}, audit_at = SYSDATETIME() " +
            "WHERE id = #{id} AND status = 'DRAFT'")
    int claimAudit(@Param("id") Long id, @Param("userId") Long userId);

    /** 状态机抢占:DRAFT→VOID 原子迁移,返回 0 表示已被驳回或不存在 */
    @Update("UPDATE sales_order SET status = 'VOID', reject_by = #{userId}, reject_at = SYSDATETIME() " +
            "WHERE id = #{id} AND status = 'DRAFT'")
    int claimReject(@Param("id") Long id, @Param("userId") Long userId);
}