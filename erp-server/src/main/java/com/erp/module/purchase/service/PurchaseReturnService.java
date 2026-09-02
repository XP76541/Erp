package com.erp.module.purchase.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.common.BusinessException;
import com.erp.module.finance.entity.Payable;
import com.erp.module.finance.mapper.PayableMapper;
import com.erp.module.inventory.service.InventoryService;
import com.erp.module.purchase.dto.PurchaseReturnDtos;
import com.erp.module.purchase.entity.PurchaseInbound;
import com.erp.module.purchase.entity.PurchaseInboundItem;
import com.erp.module.purchase.entity.PurchaseReturn;
import com.erp.module.purchase.entity.PurchaseReturnItem;
import com.erp.module.purchase.mapper.PurchaseInboundItemMapper;
import com.erp.module.purchase.mapper.PurchaseInboundMapper;
import com.erp.module.purchase.mapper.PurchaseReturnItemMapper;
import com.erp.module.purchase.mapper.PurchaseReturnMapper;
import com.erp.module.masterdata.entity.Supplier;
import com.erp.module.masterdata.mapper.SupplierMapper;
import com.erp.module.system.TokenStore;
import com.erp.module.system.service.DocSequenceService;
import com.erp.module.system.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PurchaseReturnService {
    private final PurchaseReturnMapper returnMapper;
    private final PurchaseReturnItemMapper returnItemMapper;
    private final PurchaseInboundMapper inboundMapper;
    private final PurchaseInboundItemMapper inboundItemMapper;
    private final SupplierMapper supplierMapper;
    private final InventoryService inventoryService;
    private final PayableMapper payableMapper;
    private final DocSequenceService docSequenceService;
    private final OperationLogService operationLogService;

    @Transactional
    public Long create(PurchaseReturnDtos.CreateRequest request, TokenStore.LoginUser user) {
        Supplier supplier = supplierMapper.selectById(request.getSupplierId());
        if (supplier == null || supplier.getIsActive() == 0) throw new BusinessException("供应商不存在或已停用");
        PurchaseReturn doc = new PurchaseReturn();
        LocalDate date = request.getBizDate() == null ? LocalDate.now() : request.getBizDate();
        doc.setDocNo(docSequenceService.nextDocNo("PR", "PR", date.format(DateTimeFormatter.BASIC_ISO_DATE)));
        doc.setSupplierId(request.getSupplierId()); doc.setWarehouseId(request.getWarehouseId());
        doc.setBizDate(date); doc.setStatus("DRAFT"); doc.setReason(request.getReason() == null ? "" : request.getReason()); doc.setCreatedBy(user.userId());
        returnMapper.insert(doc);
        Set<Long> sources = new HashSet<>(); int line = 1;
        for (PurchaseReturnDtos.ItemInput input : request.getItems()) {
            if (!sources.add(input.getInboundItemId())) throw new BusinessException("原入库明细不能重复");
            PurchaseInboundItem source = inboundItemMapper.selectById(input.getInboundItemId());
            if (source == null) throw new BusinessException("原入库明细不存在");
            PurchaseInbound inbound = inboundMapper.selectById(source.getInboundId());
            if (inbound == null || !"AUDITED".equals(inbound.getStatus()) || !request.getSupplierId().equals(inbound.getSupplierId())) throw new BusinessException("原入库单无效或供应商不一致");
            if (!request.getWarehouseId().equals(source.getWarehouseId())) throw new BusinessException("退货仓库必须与原入库仓库一致");
            if (input.getQty() == null || input.getQty().signum() <= 0 || input.getQty().scale() > 4) throw new BusinessException("退货数量必须大于0且最多4位小数");
            BigDecimal returned = returnItemMapper.selectList(Wrappers.<PurchaseReturnItem>lambdaQuery().eq(PurchaseReturnItem::getInboundItemId, source.getId())).stream().map(PurchaseReturnItem::getQty).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (returned.add(input.getQty()).compareTo(source.getQty()) > 0) throw new BusinessException("退货数量超过原入库可退数量");
            PurchaseReturnItem item = new PurchaseReturnItem(); item.setReturnId(doc.getId()); item.setLineNo(line++); item.setInboundItemId(source.getId()); item.setProductId(source.getProductId()); item.setWarehouseId(source.getWarehouseId()); item.setQty(input.getQty()); item.setUnitCost(source.getPrice()); item.setAmount(input.getQty().multiply(source.getPrice()).setScale(2, RoundingMode.HALF_UP)); item.setNote(input.getNote() == null ? "" : input.getNote()); returnItemMapper.insert(item);
        }
        return doc.getId();
    }

    @Transactional
    public void audit(Long id, TokenStore.LoginUser user, String ip) {
        if (returnMapper.claimAudit(id, user.userId()) == 0) throw new BusinessException("退货单不存在或不是草稿状态");
        PurchaseReturn doc = returnMapper.selectById(id);
        List<PurchaseReturnItem> items = returnItemMapper.selectList(Wrappers.<PurchaseReturnItem>lambdaQuery().eq(PurchaseReturnItem::getReturnId, id));
        if (items.isEmpty()) throw new BusinessException("退货明细不能为空");
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseReturnItem item : items) { inventoryService.stockOut("PURCHASE_RETURN", id, doc.getDocNo(), item.getProductId(), item.getWarehouseId(), item.getQty(), doc.getBizDate()); total = total.add(item.getAmount()); }
        Payable payable = new Payable(); payable.setSupplierId(doc.getSupplierId()); payable.setDocType("PURCHASE_RETURN"); payable.setDocId(id); payable.setDocNo(doc.getDocNo()); payable.setBizDate(doc.getBizDate()); payable.setDueDate(doc.getBizDate()); payable.setAmount(total.negate().setScale(2, RoundingMode.HALF_UP)); payable.setPaidAmount(BigDecimal.ZERO); payable.setStatus("UNSETTLED"); payableMapper.insert(payable);
        operationLogService.record(user, "purchase_return", "AUDIT", "PURCHASE_RETURN", id, doc.getDocNo(), "{\"amount\":" + total + "}", ip);
    }
}
