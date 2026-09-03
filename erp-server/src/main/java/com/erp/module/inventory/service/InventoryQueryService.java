package com.erp.module.inventory.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.common.PageResult;
import com.erp.module.inventory.dto.InventoryQueryDtos;
import com.erp.module.inventory.entity.Inventory;
import com.erp.module.inventory.entity.InventoryLedger;
import com.erp.module.inventory.mapper.InventoryLedgerMapper;
import com.erp.module.inventory.mapper.InventoryMapper;
import com.erp.module.masterdata.entity.Product;
import com.erp.module.masterdata.entity.Warehouse;
import com.erp.module.masterdata.mapper.ProductMapper;
import com.erp.module.masterdata.mapper.WarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 即时库存与出入库流水查询服务 */
@Service
@RequiredArgsConstructor
public class InventoryQueryService {
    private final InventoryMapper inventoryMapper;
    private final InventoryLedgerMapper ledgerMapper;
    private final ProductMapper productMapper;
    private final WarehouseMapper warehouseMapper;

    public PageResult<InventoryQueryDtos.StockResponse> stocks(Long warehouseId, Long productId, Long categoryId,
                                                               long page, long size) {
        List<Inventory> rows = inventoryMapper.selectList(Wrappers.<Inventory>lambdaQuery()
                .eq(warehouseId != null, Inventory::getWarehouseId, warehouseId)
                .eq(productId != null, Inventory::getProductId, productId)
                .orderByAsc(Inventory::getWarehouseId).orderByAsc(Inventory::getProductId));
        Map<Long, Product> products = productMapper.selectList(Wrappers.emptyWrapper()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, Warehouse> warehouses = warehouseMapper.selectList(Wrappers.emptyWrapper()).stream()
                .collect(Collectors.toMap(Warehouse::getId, Function.identity()));
        List<InventoryQueryDtos.StockResponse> result = rows.stream().filter(row -> {
            Product p = products.get(row.getProductId());
            return p != null && (categoryId == null || categoryId.equals(p.getCategoryId()));
        }).map(row -> {
            Product p = products.get(row.getProductId());
            Warehouse w = warehouses.get(row.getWarehouseId());
            InventoryQueryDtos.StockResponse dto = new InventoryQueryDtos.StockResponse();
            dto.setProductId(row.getProductId()); dto.setProductCode(p.getCode()); dto.setProductName(p.getName());
            dto.setProductSpec(p.getSpec()); dto.setCategoryId(p.getCategoryId()); dto.setWarehouseId(row.getWarehouseId());
            dto.setWarehouseName(w == null ? "" : w.getName()); dto.setQuantity(row.getQty());
            dto.setUnitCost(row.getQty().signum() == 0 ? java.math.BigDecimal.ZERO : row.getTotalCost().divide(row.getQty(), 4, java.math.RoundingMode.HALF_UP));
            dto.setTotalValue(row.getTotalCost()); return dto;
        }).toList();
        return paginate(result, page, size);
    }

    public PageResult<InventoryQueryDtos.LedgerResponse> ledgers(Long warehouseId, Long productId, String docType,
                                                                  java.time.LocalDate startDate, java.time.LocalDate endDate,
                                                                  long page, long size) {
        List<InventoryLedger> rows = ledgerMapper.selectList(Wrappers.<InventoryLedger>lambdaQuery()
                .eq(warehouseId != null, InventoryLedger::getWarehouseId, warehouseId)
                .eq(productId != null, InventoryLedger::getProductId, productId)
                .eq(docType != null && !docType.isBlank(), InventoryLedger::getDocType, docType)
                .ge(startDate != null, InventoryLedger::getBizDate, startDate)
                .le(endDate != null, InventoryLedger::getBizDate, endDate)
                .orderByDesc(InventoryLedger::getBizDate).orderByDesc(InventoryLedger::getId));
        Map<Long, Product> products = productMapper.selectList(Wrappers.emptyWrapper()).stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, Warehouse> warehouses = warehouseMapper.selectList(Wrappers.emptyWrapper()).stream().collect(Collectors.toMap(Warehouse::getId, Function.identity()));
        List<InventoryQueryDtos.LedgerResponse> result = rows.stream().map(row -> {
            InventoryQueryDtos.LedgerResponse dto = new InventoryQueryDtos.LedgerResponse();
            Product p = products.get(row.getProductId()); Warehouse w = warehouses.get(row.getWarehouseId());
            dto.setId(row.getId()); dto.setDocType(row.getDocType()); dto.setDocId(row.getDocId()); dto.setDocNo(row.getDocNo());
            dto.setProductId(row.getProductId()); dto.setProductName(p == null ? "" : p.getName()); dto.setWarehouseId(row.getWarehouseId());
            dto.setWarehouseName(w == null ? "" : w.getName()); dto.setDirection(row.getDirection()); dto.setQuantity(row.getQty());
            dto.setUnitCost(row.getUnitCost()); dto.setAmount(row.getAmount()); dto.setBalanceQuantity(row.getBalanceQty());
            dto.setBalanceAmount(row.getBalanceAmount()); dto.setBizDate(row.getBizDate()); dto.setCreatedAt(row.getCreatedAt()); return dto;
        }).toList();
        return paginate(result, page, size);
    }

    private <T> PageResult<T> paginate(List<T> rows, long page, long size) {
        long safePage = Math.max(page, 1), safeSize = Math.max(size, 1), from = (safePage - 1) * safeSize;
        if (from >= rows.size()) return PageResult.of(rows.size(), List.of());
        int end = (int) Math.min(rows.size(), from + safeSize);
        return PageResult.of(rows.size(), rows.subList((int) from, end));
    }
}
