package com.erp.module.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.purchase.entity.PurchaseReturn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PurchaseReturnMapper extends BaseMapper<PurchaseReturn> {
    @Update("UPDATE purchase_return SET status = 'AUDITED', audit_by = #{userId}, audit_at = SYSDATETIME() WHERE id = #{id} AND status = 'DRAFT'")
    int claimAudit(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT COALESCE(SUM(amount), 0) FROM purchase_return_item WHERE return_id = #{id}")
    java.math.BigDecimal selectTotalAmount(@Param("id") Long id);

    @Select("SELECT pr.*, COALESCE(x.total_amount, 0) AS total_amount FROM purchase_return pr "
            + "LEFT JOIN (SELECT return_id, SUM(amount) AS total_amount FROM purchase_return_item GROUP BY return_id) x "
            + "ON x.return_id = pr.id WHERE pr.id = #{id}")
    PurchaseReturn selectWithTotal(@Param("id") Long id);

    @Select("SELECT pr.*, COALESCE(x.total_amount, 0) AS total_amount FROM purchase_return pr "
            + "LEFT JOIN (SELECT return_id, SUM(amount) AS total_amount FROM purchase_return_item GROUP BY return_id) x "
            + "ON x.return_id = pr.id ORDER BY pr.id DESC")
    List<PurchaseReturn> selectAllWithTotal();
}
