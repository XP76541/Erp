package com.erp.module.purchase.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    public PageResult<PurchaseReturnDtos.ListResponse> page(long page, long size, String keyword, String status, Long supplierId) {
        var query = Wrappers.<PurchaseReturn>lambdaQuery()
                .like(org.springframework.util.StringUtils.hasText(keyword), PurchaseReturn::getDocNo, keyword)
                .eq(org.springframework.util.StringUtils.hasText(status), PurchaseReturn::getStatus, status)
                .eq(supplierId != null, PurchaseReturn::getSupplierId, supplierId)
                .orderByDesc(PurchaseReturn::getId);
        Page<PurchaseReturn> result = returnMapper.selectPage(new Page<>(Math.max(page, 1), Math.max(size, 1)), query);
        List<PurchaseReturnDtos.ListResponse> records = result.getRecords().stream().map(doc -> {
            doc.setTotalAmount(returnMapper.selectTotalAmount(doc.getId()));
            return toListResponse(doc);
        }).toList();
        return PageResult.of(result.getTotal(), records);
    }

    public PurchaseReturnDtos.DetailResponse detail(Long id) {
        PurchaseReturn doc = returnMapper.selectWithTotal(id);
        if (doc == null) throw new BusinessException("采购退货单不存在");
        List<PurchaseReturnItem> items = returnItemMapper.selectList(Wrappers.<PurchaseReturnItem>lambdaQuery()
                .eq(PurchaseReturnItem::getReturnId, id).orderByAsc(PurchaseReturnItem::getLineNo));
        return new PurchaseReturnDtos.DetailResponse(doc, items);
    }

    private PurchaseReturnDtos.ListResponse toListResponse(PurchaseReturn doc) {
        PurchaseReturnDtos.ListResponse response = new PurchaseReturnDtos.ListResponse();
        response.setId(doc.getId()); response.setDocNo(doc.getDocNo()); response.setSupplierId(doc.getSupplierId());
        response.setWarehouseId(doc.getWarehouseId()); response.setBizDate(doc.getBizDate()); response.setStatus(doc.getStatus());
        response.setReason(doc.getReason()); response.setTotalAmount(doc.getTotalAmount() == null ? BigDecimal.ZERO : doc.getTotalAmount());
        response.setAuditBy(doc.getAuditBy()); response.setAuditAt(doc.getAuditAt()); response.setCreatedAt(doc.getCreatedAt());
        return response;
    }

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
        List<PurchaseReturnDtos.ItemInput> inputs = request.getItems().stream()
                .sorted(java.util.Comparator.comparing(PurchaseReturnDtos.ItemInput::getInboundItemId))
                .toList();
        for (PurchaseReturnDtos.ItemInput input : inputs) {
            if (!sources.add(input.getInboundItemId())) throw new BusinessException("原入库明细不能重复");
            PurchaseInboundItem source = inboundItemMapper.selectForUpdate(input.getInboundItemId());
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
        items = items.stream().sorted(Comparator.comparing(PurchaseReturnItem::getInboundItemId)).collect(Collectors.toList());
        for (PurchaseReturnItem item : items) {
            PurchaseInboundItem source = inboundItemMapper.selectForUpdate(item.getInboundItemId());
            if (source == null || source.getQty() == null) throw new BusinessException("原入库明细不存在");
            BigDecimal returned = returnItemMapper.selectList(Wrappers.<PurchaseReturnItem>lambdaQuery()
                    .eq(PurchaseReturnItem::getInboundItemId, item.getInboundItemId())).stream()
                    .map(PurchaseReturnItem::getQty).filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (returned.compareTo(source.getQty()) > 0) throw new BusinessException("退货数量超过原入库可退数量");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseReturnItem item : items) { inventoryService.stockOut("PURCHASE_RETURN", id, doc.getDocNo(), item.getProductId(), item.getWarehouseId(), item.getQty(), doc.getBizDate()); total = total.add(item.getAmount()); }
        Payable payable = new Payable(); payable.setSupplierId(doc.getSupplierId()); payable.setDocType("PURCHASE_RETURN"); payable.setDocId(id); payable.setDocNo(doc.getDocNo()); payable.setBizDate(doc.getBizDate()); payable.setDueDate(doc.getBizDate()); payable.setAmount(total.negate().setScale(2, RoundingMode.HALF_UP)); payable.setPaidAmount(BigDecimal.ZERO); payable.setStatus("UNSETTLED"); payableMapper.insert(payable);
        operationLogService.record(user, "purchase_return", "AUDIT", "PURCHASE_RETURN", id, doc.getDocNo(), "{\"amount\":" + total + "}", ip);
    }
}
