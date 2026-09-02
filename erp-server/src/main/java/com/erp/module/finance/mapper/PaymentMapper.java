package com.erp.module.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Update;
import lombok.Data;
import com.erp.module.finance.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {

    /**
     * 获取客户待收款统计
     */
    @Select("SELECT " +
            "customer_id, " +
            "SUM(amount) as total_amount, " +
            "SUM(allocated_amount) as total_allocated, " +
            "SUM(amount - allocated_amount) as total_remaining " +
            "FROM payment " +
            "WHERE status = 'DRAFT' " +
            "GROUP BY customer_id")
    List<PaymentStatistics> getCustomerPaymentStatistics(@Param("customerId") Long customerId);

    /**
     * 获取未核销的收款单
     */
    @Select("SELECT * FROM payment WHERE status = 'DRAFT' ORDER BY created_at")
    List<Payment> getUnallocatedPayments();

    /**
     * 原子状态更新
     */
    @Update("UPDATE payment SET status = #{status}, updated_at = SYSDATETIME() WHERE id = #{id} AND status = 'DRAFT'")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Data
    static class PaymentStatistics {
        private Long customerId;
        private BigDecimal totalAmount;
        private BigDecimal totalAllocated;
        private BigDecimal totalRemaining;
    }
}