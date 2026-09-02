package com.erp.module.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.finance.entity.PaymentAllocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface PaymentAllocationMapper extends BaseMapper<PaymentAllocation> {
    @Select("SELECT COALESCE(SUM(amount), 0) FROM payment_allocation WHERE payment_id = #{paymentId}")
    BigDecimal getAllocatedAmount(@Param("paymentId") Long paymentId);

    @Select("SELECT * FROM payment_allocation WHERE payment_id = #{paymentId}")
    List<PaymentAllocation> getByPaymentId(@Param("paymentId") Long paymentId);
}
