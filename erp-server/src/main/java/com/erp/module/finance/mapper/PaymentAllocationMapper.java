package com.erp.module.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import com.erp.module.finance.entity.PaymentAllocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface PaymentAllocationMapper extends BaseMapper<PaymentAllocation> {

    /**
     * 获取收款单的核销明细
     */
    List<PaymentAllocation> getByPaymentId(@Param("paymentId") Long paymentId);

    /**
     * 计算收款单已核销金额
     */
    @Select("SELECT COALESCE(SUM(allocated_amount), 0) FROM payment_allocation WHERE payment_id = #{paymentId}")
    BigDecimal getAllocatedAmount(@Param("paymentId") Long paymentId);

    /**
     * 获取应收账款的核销记录
     */
    List<PaymentAllocation> getByReceivableId(@Param("receivableId") Long receivableId);

    /**
     * 检查应收账款是否已被完全核销
     */
    @Select("SELECT r.remaining_amount FROM receivable r " +
            "LEFT JOIN payment_allocation pa ON r.id = pa.receivable_id " +
            "WHERE r.id = #{receivableId} AND r.remaining_amount > 0")
    BigDecimal getReceivableRemainingAmount(@Param("receivableId") Long receivableId);
}