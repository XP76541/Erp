package com.erp.module.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.purchase.entity.PurchaseInbound;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface PurchaseInboundMapper extends BaseMapper<PurchaseInbound> {

    /** 状态机抢占:DRAFT→AUDITED 原子迁移,返回 0 表示已被审或不存在 */
    @Update("UPDATE purchase_inbound SET status = 'AUDITED', audit_by = #{userId}, audit_at = SYSDATETIME() "
            + "WHERE id = #{id} AND status = 'DRAFT'")
    int claimAudit(@Param("id") Long id, @Param("userId") Long userId);
}
