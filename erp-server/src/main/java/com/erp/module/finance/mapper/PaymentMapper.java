package com.erp.module.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.finance.entity.Payment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface PaymentMapper extends BaseMapper<Payment> {
    @Update("UPDATE payment SET status = 'AUDITED', audit_by = #{userId}, audit_at = SYSDATETIME() " +
            "WHERE id = #{id} AND status = 'DRAFT'")
    int claimAudit(@Param("id") Long id, @Param("userId") Long userId);
}