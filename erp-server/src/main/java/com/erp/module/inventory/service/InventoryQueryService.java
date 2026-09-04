package com.erp.module.inventory.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.BusinessException;
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

import java.time.LocalDate;
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
        long safePage = Math.max(page, 1), safeSize = Math.max(size, 1);
        Page<Inventory> result;
        if (categoryId != null) {
            List<Long> productIds = productMapper.selectList(Wrappers.<Product>lambdaQuery()
                            .eq(Product::getCategoryId, categoryId))
                    .stream().map(Product::getId).toList();
            if (productIds.isEmpty() || (productId != null && !productIds.contains(productId))) {
                return PageResult.of(0, List.of());
            }
            result = inventoryMapper.selectPage(new Page<>(safePage, safeSize),
                    Wrappers.<Inventory>lambdaQuery()
                            .eq(warehouseId != null, Inventory::getWarehouseId, warehouseId)
                            .eq(productId != null, Inventory::getProductId, productId)
                            .in(Inventory::getProductId, productIds)
                            .orderByAsc(Inventory::getWarehouseId).orderByAsc(Inventory::getProductId));
        } else {
            result = inventoryMapper.selectPage(new Page<>(safePage, safeSize),
                    Wrappers.<Inventory>lambdaQuery()
                            .eq(warehouseId != null, Inventory::getWarehouseId, warehouseId)
                            .eq(productId != null, Inventory::getProductId, productId)
                            .orderByAsc(Inventory::getWarehouseId).orderByAsc(Inventory::getProductId));
        }
        List<Inventory> rows = result.getRecords();
        Map<Long, Product> products = productMapper.selectBatchIds(rows.stream()
                        .map(Inventory::getProductId).distinct().toList()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, Warehouse> warehouses = warehouseMapper.selectBatchIds(rows.stream()
                        .map(Inventory::getWarehouseId).distinct().toList()).stream()
                .collect(Collectors.toMap(Warehouse::getId, Function.identity()));
        List<InventoryQueryDtos.StockResponse> records = rows.stream().map(row -> {
            Product p = products.get(row.getProductId());
            Warehouse w = warehouses.get(row.getWarehouseId());
            InventoryQueryDtos.StockResponse dto = new InventoryQueryDtos.StockResponse();
            dto.setProductId(row.getProductId()); dto.setProductCode(p == null ? "" : p.getCode());
            dto.setProductName(p == null ? "" : p.getName()); dto.setProductSpec(p == null ? "" : p.getSpec());
            dto.setCategoryId(p == null ? null : p.getCategoryId()); dto.setWarehouseId(row.getWarehouseId());
            dto.setWarehouseName(w == null ? "" : w.getName()); dto.setQuantity(row.getQty());
            dto.setUnitCost(row.getQty() == null || row.getQty().signum() == 0 ? java.math.BigDecimal.ZERO
                    : row.getTotalCost().divide(row.getQty(), 4, java.math.RoundingMode.HALF_UP));
            dto.setTotalValue(row.getTotalCost()); return dto;
        }).toList();
        return PageResult.of(result.getTotal(), records);
    }

    public PageResult<InventoryQueryDtos.LedgerResponse> ledgers(Long warehouseId, Long productId, String docType,
                                                                  LocalDate startDate, LocalDate endDate,
                                                                  long page, long size) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(400, "开始日期不能晚于结束日期");
        }
        Page<InventoryLedger> result = ledgerMapper.selectPage(new Page<>(Math.max(page, 1), Math.max(size, 1)),
                Wrappers.<InventoryLedger>lambdaQuery()
                        .eq(warehouseId != null, InventoryLedger::getWarehouseId, warehouseId)
                        .eq(productId != null, InventoryLedger::getProductId, productId)
                        .eq(docType != null && !docType.isBlank(), InventoryLedger::getDocType, docType.trim())
                        .ge(startDate != null, InventoryLedger::getBizDate, startDate)
                        .le(endDate != null, InventoryLedger::getBizDate, endDate)
                        .orderByDesc(InventoryLedger::getBizDate)
                        .orderByDesc(InventoryLedger::getCreatedAt)
                        .orderByDesc(InventoryLedger::getId));
        List<InventoryLedger> rows = result.getRecords();
        Map<Long, Product> products = productMapper.selectBatchIds(rows.stream()
                        .map(InventoryLedger::getProductId).distinct().toList()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, Warehouse> warehouses = warehouseMapper.selectBatchIds(rows.stream()
                        .map(InventoryLedger::getWarehouseId).distinct().toList()).stream()
                .collect(Collectors.toMap(Warehouse::getId, Function.identity()));
        List<InventoryQueryDtos.LedgerResponse> records = rows.stream().map(row -> {
            InventoryQueryDtos.LedgerResponse dto = new InventoryQueryDtos.LedgerResponse();
            Product p = products.get(row.getProductId()); Warehouse w = warehouses.get(row.getWarehouseId());
            dto.setId(row.getId()); dto.setDocType(row.getDocType()); dto.setDocId(row.getDocId()); dto.setDocNo(row.getDocNo());
            dto.setProductId(row.getProductId()); dto.setProductName(p == null ? "" : p.getName()); dto.setWarehouseId(row.getWarehouseId());
            dto.setWarehouseName(w == null ? "" : w.getName()); dto.setDirection(row.getDirection()); dto.setQuantity(row.getQty());
            dto.setUnitCost(row.getUnitCost()); dto.setAmount(row.getAmount()); dto.setBalanceQuantity(row.getBalanceQty());
            dto.setBalanceAmount(row.getBalanceAmount()); dto.setBizDate(row.getBizDate()); dto.setCreatedAt(row.getCreatedAt()); return dto;
        }).toList();
        return PageResult.of(result.getTotal(), records);
    }
}
