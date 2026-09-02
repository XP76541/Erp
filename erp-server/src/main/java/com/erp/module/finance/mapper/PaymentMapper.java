package com.erp.module.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.finance.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
    @Update("UPDATE payment SET status = #{status}, audit_by = #{userId}, audit_at = SYSDATETIME(), updated_by = #{userId}, updated_at = SYSDATETIME() WHERE id = #{id} AND status = 'DRAFT'")
    int claimAudit(@Param("id") Long id, @Param("status") String status, @Param("userId") Long userId);

    @Select("SELECT COALESCE(SUM(amount), 0) FROM payment_allocation WHERE payment_id = #{paymentId}")
    BigDecimal getAllocatedAmount(@Param("paymentId") Long paymentId);
}
