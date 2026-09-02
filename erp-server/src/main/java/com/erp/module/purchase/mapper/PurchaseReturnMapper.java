package com.erp.module.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.purchase.entity.PurchaseReturn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PurchaseReturnMapper extends BaseMapper<PurchaseReturn> {
    @Update("UPDATE purchase_return SET status = 'AUDITED', audit_by = #{userId}, audit_at = SYSDATETIME() WHERE id = #{id} AND status = 'DRAFT'")
    int claimAudit(@Param("id") Long id, @Param("userId") Long userId);
}
