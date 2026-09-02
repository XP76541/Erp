package com.erp.module.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.purchase.entity.PurchaseInboundItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PurchaseInboundItemMapper extends BaseMapper<PurchaseInboundItem> {

    /** 锁定来源入库明细,确保退货额度检查与插入处于同一事务 */
    @Select("SELECT * FROM purchase_inbound_item WITH (UPDLOCK, HOLDLOCK, ROWLOCK) WHERE id = #{id}")
    PurchaseInboundItem selectForUpdate(@Param("id") Long id);
}
