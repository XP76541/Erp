package com.erp.module.inventory.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.module.inventory.entity.Inventory;
import com.erp.module.inventory.entity.InventoryLedger;
import com.erp.module.inventory.mapper.InventoryLedgerMapper;
import com.erp.module.inventory.mapper.InventoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 库存核心:数量与成本的一切变动只能经过本服务(已审核单据调用)
 *
 * 入库:新加权成本 = (结存金额 + 本次入库金额) / (结存数量 + 本次入库数量)
 * 出库:结转成本 = 出库数量 × 当前加权成本(出库不改变加权成本)
 * 见 docs/database-design.md §6.3
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryMapper inventoryMapper;
    private final InventoryLedgerMapper ledgerMapper;

    /**
     * 入库(采购入库/退货入库/调拨入/盘盈等):结存增加,重算加权平均,写台账
     * 必须在调用方事务内执行;行锁保证同一结存行串行
     */
    @Transactional
    public void stockIn(String docType, Long docId, String docNo,
                        Long productId, Long warehouseId, BigDecimal qty, BigDecimal price,
                        LocalDate bizDate) {
        Inventory inv = inventoryMapper.selectForUpdate(productId, warehouseId);
        BigDecimal amount = qty.multiply(price).setScale(2, RoundingMode.HALF_UP);

        BigDecimal newQty;
        BigDecimal newAmount;
        if (inv == null) {
            // 首次入库:直接建结存行
            newQty = qty;
            newAmount = amount;
            Inventory created = new Inventory();
            created.setProductId(productId);
            created.setWarehouseId(warehouseId);
            created.setQty(newQty);
            created.setTotalCost(newAmount);
            created.setVersion(0);
            inventoryMapper.insert(created);
        } else {
            newQty = inv.getQty().add(qty);
            newAmount = inv.getTotalCost().add(amount);
            Inventory update = new Inventory();
            update.setId(inv.getId());
            update.setQty(newQty);
            update.setTotalCost(newAmount);
            update.setVersion(inv.getVersion() + 1);
            inventoryMapper.updateById(update);
        }

        LedgerEntry ledger = new LedgerEntry();
        ledger.direction = 1;
        ledger.qty = qty;
        ledger.unitCost = price;
        ledger.amount = amount;
        ledger.balanceQty = newQty;
        ledger.balanceAmount = newAmount;
        writeLedger(docType, docId, docNo, productId, warehouseId, ledger, bizDate);
    }

    /**
     * 查询指定商品在指定仓库的库存
     */
    public BigDecimal getStockQuantity(Long productId, Long warehouseId) {
        Inventory inventory = inventoryMapper.selectOne(
                Wrappers.<Inventory>lambdaQuery()
                        .eq(Inventory::getProductId, productId)
                        .eq(Inventory::getWarehouseId, warehouseId));

        return inventory != null ? inventory.getQty() : BigDecimal.ZERO;
    }

    /**
     * 查询指定仓库的所有商品库存
     */
    public Map<Long, BigDecimal> getWarehouseStocks(Long warehouseId) {
        List<Inventory> inventories = inventoryMapper.selectList(
                Wrappers.<Inventory>lambdaQuery()
                        .eq(Inventory::getWarehouseId, warehouseId));

        return inventories.stream()
                .collect(Collectors.toMap(
                        Inventory::getProductId,
                        Inventory::getQty
                ));
    }

    /**
     * 查询指定商品的单位成本
     */
    public BigDecimal getUnitCost(Long productId, Long warehouseId) {
        Inventory inventory = inventoryMapper.selectOne(
                Wrappers.<Inventory>lambdaQuery()
                        .eq(Inventory::getProductId, productId)
                        .eq(Inventory::getWarehouseId, warehouseId));

        if (inventory == null || inventory.getQty().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return inventory.getTotalCost().divide(inventory.getQty(), 4, RoundingMode.HALF_UP);
    }

    /**
     * 查询指定仓库的库存总价值
     */
    public BigDecimal getWarehouseTotalValue(Long warehouseId) {
        List<Inventory> inventories = inventoryMapper.selectList(
                Wrappers.<Inventory>lambdaQuery()
                        .eq(Inventory::getWarehouseId, warehouseId));

        return inventories.stream()
                .map(inv -> inv.getTotalCost())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 出库(销售出库/退货出库/调拨出/盘亏等):按当前加权平均结转成本;库存不足整体失败 */
    @Transactional
    public void stockOut(String docType, Long docId, String docNo,
                         Long productId, Long warehouseId, BigDecimal qty,
                         LocalDate bizDate) {
        Inventory inv = inventoryMapper.selectForUpdate(productId, warehouseId);
        if (inv == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                    "库存不足:商品 " + productId + " 在仓库 " + warehouseId + " 无结存,需出库 " + qty);
        }
        BigDecimal available = inv.getQty();
        if (available.compareTo(qty) < 0) {
            throw new IllegalStateException(
                    "库存不足:商品 " + productId + " 在仓库 " + warehouseId + " 结存 " + available + ",需出库 " + qty);
        }
        BigDecimal avgCost = inv.getTotalCost().divide(available, 4, RoundingMode.HALF_UP);
        BigDecimal amount = qty.multiply(avgCost).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newQty = available.subtract(qty);
        BigDecimal newAmount = inv.getTotalCost().subtract(amount);
        if (newQty.compareTo(BigDecimal.ZERO) == 0) {
            // 清仓时把尾差一并结转,避免结存数量 0 但金额残留分位尾差
            newAmount = BigDecimal.ZERO.setScale(2);
        }

        Inventory update = new Inventory();
        update.setId(inv.getId());
        update.setQty(newQty);
        update.setTotalCost(newAmount);
        update.setVersion(inv.getVersion() + 1);
        inventoryMapper.updateById(update);

        LedgerEntry ledger = new LedgerEntry();
        ledger.direction = -1;
        ledger.qty = qty;
        ledger.unitCost = avgCost.setScale(2, RoundingMode.HALF_UP);
        ledger.amount = amount;
        ledger.balanceQty = newQty;
        ledger.balanceAmount = newAmount;
        writeLedger(docType, docId, docNo, productId, warehouseId, ledger, bizDate);
    }

    private void writeLedger(String docType, Long docId, String docNo,
                             Long productId, Long warehouseId, LedgerEntry entry, LocalDate bizDate) {
        InventoryLedger row = new InventoryLedger();
        row.setDocType(docType);
        row.setDocId(docId);
        row.setDocNo(docNo);
        row.setProductId(productId);
        row.setWarehouseId(warehouseId);
        row.setDirection(entry.direction);
        row.setQty(entry.qty);
        row.setUnitCost(entry.unitCost);
        row.setAmount(entry.amount);
        row.setBalanceQty(entry.balanceQty);
        row.setBalanceAmount(entry.balanceAmount);
        row.setBizDate(bizDate);
        ledgerMapper.insert(row);
    }

    /** 台账单行数值载体 */
    private static class LedgerEntry {
        int direction;
        BigDecimal qty;
        BigDecimal unitCost;
        BigDecimal amount;
        BigDecimal balanceQty;
        BigDecimal balanceAmount;
    }
}
