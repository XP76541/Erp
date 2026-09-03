package com.erp.module.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Update;
import lombok.Data;
import com.erp.module.finance.entity.Receivable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ReceivableMapper extends BaseMapper<Receivable> {

    /** 查询客户当前未核销应收余额，用于销售审核信用控制 */
    @Select("SELECT COALESCE(SUM(remaining_amount), 0) FROM receivable " +
            "WHERE customer_id = #{customerId} AND status IN ('UNSETTLED', 'PARTIAL') " +
            "AND remaining_amount > 0")
    BigDecimal sumOutstandingByCustomerId(@Param("customerId") Long customerId);

    @Select("SELECT * FROM receivable WHERE customer_id = #{customerId} AND status IN ('UNSETTLED', 'PARTIAL') ORDER BY due_date")
    List<Receivable> getUnsettledByCustomerId(@Param("customerId") Long customerId);

    /**
     * 获取指定日期范围内的应收账款
     */
    @Select("SELECT * FROM receivable WHERE business_date BETWEEN #{startDate} AND #{endDate} ORDER BY business_date")
    List<Receivable> getByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 更新付款金额和状态
     */
    @Update("UPDATE receivable SET received_amount = received_amount + #{amount}, " +
            "remaining_amount = amount - (received_amount + #{amount}), " +
            "status = CASE " +
            "   WHEN amount - (received_amount + #{amount}) <= 0 THEN 'SETTLED' " +
            "   WHEN received_amount + #{amount} > 0 THEN 'PARTIAL' " +
            "   ELSE 'UNSETTLED' " +
            "END " +
            "WHERE id = #{id} AND amount - received_amount >= #{amount}")
    int updatePaidAmount(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /**
     * 获取客户应收账款统计
     */
    @Select("SELECT " +
            "customer_id, " +
            "SUM(amount) as total_amount, " +
            "SUM(paid_amount) as total_paid, " +
            "SUM(remaining_amount) as total_remaining, " +
            "SUM(CASE WHEN status = 'UNSETTLED' THEN remaining_amount ELSE 0 END) as unsettled_amount, " +
            "SUM(CASE WHEN status = 'PARTIAL' THEN remaining_amount ELSE 0 END) as partial_amount, " +
            "SUM(CASE WHEN status = 'SETTLED' THEN remaining_amount ELSE 0 END) as settled_amount " +
            "FROM receivable " +
            "GROUP BY customer_id")
    List<ReceivableStatistics> getCustomerReceivableStatistics();

    /**
     * 获取账龄分析数据（截止日期由调用方明确传入，避免历史查询依赖服务器当前日期）。
     */
    @Select({"<script>",
            "SELECT CASE ",
            "   WHEN DATEDIFF(day, due_date, #{cutoffDate}) &lt; 0 THEN '未到期' ",
            "   WHEN DATEDIFF(day, due_date, #{cutoffDate}) BETWEEN 0 AND 30 THEN '1-30天' ",
            "   WHEN DATEDIFF(day, due_date, #{cutoffDate}) BETWEEN 31 AND 60 THEN '31-60天' ",
            "   WHEN DATEDIFF(day, due_date, #{cutoffDate}) BETWEEN 61 AND 90 THEN '61-90天' ",
            "   ELSE '90天以上' END as aging_bucket, ",
            "SUM(amount) as total_amount, SUM(received_amount) as total_paid, ",
            "SUM(remaining_amount) as total_remaining ",
            "FROM receivable ",
            "WHERE remaining_amount > 0 ",
            "<if test='customerIds != null and customerIds.size() > 0'>",
            "AND customer_id IN ",
            "<foreach collection='customerIds' item='customerId' open='(' separator=',' close=')'>#{customerId}</foreach>",
            "</if>",
            "GROUP BY CASE ",
            "   WHEN DATEDIFF(day, due_date, #{cutoffDate}) &lt; 0 THEN '未到期' ",
            "   WHEN DATEDIFF(day, due_date, #{cutoffDate}) BETWEEN 0 AND 30 THEN '1-30天' ",
            "   WHEN DATEDIFF(day, due_date, #{cutoffDate}) BETWEEN 31 AND 60 THEN '31-60天' ",
            "   WHEN DATEDIFF(day, due_date, #{cutoffDate}) BETWEEN 61 AND 90 THEN '61-90天' ",
            "   ELSE '90天以上' END",
            "</script>"})
    List<AgingAnalysis> getAgingAnalysis(@Param("cutoffDate") LocalDate cutoffDate,
                                         @Param("customerIds") List<Long> customerIds);

    /** 兼容旧调用，默认使用当天且不限制客户范围。 */
    default List<AgingAnalysis> getAgingAnalysis() {
        return getAgingAnalysis(LocalDate.now(), null);
    }

    /** 获取截止日期前的逾期应收账款。 */
    @Select({"<script>",
            "SELECT * FROM receivable ",
            "WHERE due_date &lt; #{cutoffDate} AND remaining_amount > 0 ",
            "<if test='customerIds != null and customerIds.size() > 0'>",
            "AND customer_id IN ",
            "<foreach collection='customerIds' item='customerId' open='(' separator=',' close=')'>#{customerId}</foreach>",
            "</if>",
            "ORDER BY DATEDIFF(day, due_date, #{cutoffDate}) DESC",
            "</script>"})
    List<Receivable> getOverdueReceivables(@Param("cutoffDate") LocalDate cutoffDate,
                                           @Param("customerIds") List<Long> customerIds);

    /** 兼容旧调用，默认使用当天且不限制客户范围。 */
    default List<Receivable> getOverdueReceivables() {
        return getOverdueReceivables(LocalDate.now(), null);
    }

    @Data
    static class ReceivableStatistics {
        private Long customerId;
        private BigDecimal totalAmount;
        private BigDecimal totalPaid;
        private BigDecimal totalRemaining;
        private BigDecimal unsettledAmount;
        private BigDecimal partialAmount;
        private BigDecimal settledAmount;
    }

    @Data
    static class AgingAnalysis {
        private String agingBucket;
        private BigDecimal totalAmount;
        private BigDecimal totalPaid;
        private BigDecimal totalRemaining;
    }
}