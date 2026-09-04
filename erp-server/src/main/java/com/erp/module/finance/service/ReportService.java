package com.erp.module.finance.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.module.masterdata.entity.Customer;
import com.erp.module.masterdata.entity.Product;
import com.erp.module.masterdata.entity.Warehouse;
import com.erp.module.masterdata.mapper.CustomerMapper;
import com.erp.module.masterdata.mapper.ProductMapper;
import com.erp.module.masterdata.mapper.WarehouseMapper;
import com.erp.module.purchase.entity.PurchaseInbound;
import com.erp.module.purchase.mapper.PurchaseInboundItemMapper;
import com.erp.module.purchase.mapper.PurchaseInboundMapper;
import com.erp.module.sales.entity.SalesOutbound;
import com.erp.module.sales.entity.SalesOutboundItem;
import com.erp.module.sales.mapper.SalesOutboundMapper;
import com.erp.module.sales.mapper.SalesOutboundItemMapper;
import com.erp.module.sales.mapper.SalesOrderMapper;
import com.erp.module.finance.entity.Payable;
import com.erp.module.finance.mapper.PayableMapper;
import com.erp.module.finance.entity.Receivable;
import com.erp.module.finance.mapper.ReceivableMapper;
import com.erp.module.finance.dto.ReportDtos;
import com.erp.module.inventory.entity.InventoryLedger;
import com.erp.module.inventory.mapper.InventoryLedgerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 报表服务
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final SalesOutboundMapper outboundMapper;
    private final SalesOutboundItemMapper outboundItemMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final PurchaseInboundMapper purchaseInboundMapper;
    private final PurchaseInboundItemMapper purchaseInboundItemMapper;
    private final ReceivableMapper receivableMapper;
    private final PayableMapper payableMapper;
    private final CustomerMapper customerMapper;
    private final ProductMapper productMapper;
    private final WarehouseMapper warehouseMapper;
    private final InventoryLedgerMapper inventoryLedgerMapper;

    /**
     * 销售日报表
     */
    public List<ReportDtos.SalesDailyReportResponse> getSalesDailyReport(ReportDtos.SalesDailyReportRequest request) {
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1); // 默认本月1号
        }
        if (endDate == null) {
            endDate = LocalDate.now(); // 默认今天
        }

        // 查询日期范围内的出库单
        List<SalesOutbound> outbounds = outboundMapper.selectByDateRange(startDate, endDate);

        // 按日期分组
        Map<LocalDate, List<SalesOutbound>> dateGroup = outbounds.stream()
                .collect(Collectors.groupingBy(SalesOutbound::getBizDate));

        List<ReportDtos.SalesDailyReportResponse> responses = new ArrayList<>();

        for (LocalDate date : dateGroup.keySet().stream().sorted().collect(Collectors.toList())) {
            ReportDtos.SalesDailyReportResponse response = new ReportDtos.SalesDailyReportResponse();
            response.setReportDate(date);

            List<SalesOutbound> dayOutbounds = dateGroup.get(date).stream()
                    .filter(outbound -> request.getCustomerId() == null || request.getCustomerId().equals(outbound.getCustomerId()))
                    .filter(outbound -> request.getSalespersonId() == null || hasSalesperson(outbound, request.getSalespersonId()))
                    .filter(outbound -> "AUDITED".equals(outbound.getStatus()))
                    .toList();
            Long totalOrders = (long) dayOutbounds.size();

            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal shippedAmount = BigDecimal.ZERO;
            List<ReportDtos.SalesDailyReportItem> items = new ArrayList<>();

            for (SalesOutbound outbound : dayOutbounds) {
                ReportDtos.SalesDailyReportItem item = new ReportDtos.SalesDailyReportItem();
                item.setDocNo(outbound.getDocNo());
                item.setBusinessDate(outbound.getBizDate());
                item.setAmount(outbound.getTotalAmount());
                totalAmount = totalAmount.add(outbound.getTotalAmount());

                // 获取客户信息
                Customer customer = customerMapper.selectById(outbound.getCustomerId());
                item.setCustomerName(customer != null ? customer.getName() : "");

                // 获取出库明细计算已发货金额
                List<SalesOutboundItem> outboundItems = outboundItemMapper.selectByOutboundId(outbound.getId());
                BigDecimal dayShippedAmount = outboundItems.stream()
                        .map(SalesOutboundItem::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                item.setShippedAmount(dayShippedAmount);
                shippedAmount = shippedAmount.add(dayShippedAmount);

                item.setStatus(outbound.getStatus());
                items.add(item);
            }

            response.setTotalOrders(totalOrders);
            response.setTotalAmount(totalAmount);
            response.setShippedAmount(shippedAmount);
            response.setOrders(items);
            responses.add(response);
        }

        return responses;
    }

    /**
     * 进销存汇总报表
     */
    public ReportDtos.InventorySummaryResponse getInventorySummary(ReportDtos.InventorySummaryRequest request) {
        LocalDate date = request.getDate();
        if (date == null) {
            date = LocalDate.now(); // 默认今天
        }

        ReportDtos.InventorySummaryResponse response = new ReportDtos.InventorySummaryResponse();
        response.setReportDate(date);

        // 查询所有仓库
        List<Warehouse> warehouses = warehouseMapper.selectList(Wrappers.emptyWrapper());

        List<ReportDtos.InventorySummaryItem> items = new ArrayList<>();
        BigDecimal totalValue = BigDecimal.ZERO;

        for (Warehouse warehouse : warehouses) {
            // 如果指定了仓库，跳过其他仓库
            if (request.getWarehouseId() != null && !warehouse.getId().equals(request.getWarehouseId())) {
                continue;
            }

            // 获取该仓库所有商品的库存
            // 这里假设有一个方法可以查询指定仓库的库存，如果没有，需要从inventory表查询
            Map<Long, InventoryLedger> warehouseStocks = getStocksByWarehouse(warehouse.getId(), date);

            for (Map.Entry<Long, InventoryLedger> entry : warehouseStocks.entrySet()) {
                Long productId = entry.getKey();
                InventoryLedger snapshot = entry.getValue();
                BigDecimal quantity = safe(snapshot.getBalanceQty());

                // 如果指定了商品，跳过其他商品
                if (request.getProductId() != null && !productId.equals(request.getProductId())) {
                    continue;
                }

                Product product = productMapper.selectById(productId);
                if (product == null || product.getIsActive() == 0) {
                    continue;
                }

                // 使用截至报表日期的流水结存快照，不读取即时库存表。
                BigDecimal unitCost = quantity.signum() == 0 ? BigDecimal.ZERO
                        : safe(snapshot.getBalanceAmount()).divide(quantity, 4, RoundingMode.HALF_UP);

                ReportDtos.InventorySummaryItem item = new ReportDtos.InventorySummaryItem();
                item.setProductId(productId);
                item.setProductName(product.getName());
                item.setProductSpec(product.getSpec() != null ? product.getSpec() : "");
                item.setWarehouseId(warehouse.getId());
                item.setWarehouseName(warehouse.getName());
                item.setQuantity(quantity);
                item.setUnitCost(unitCost);
                BigDecimal totalValueForItem = safe(snapshot.getBalanceAmount());
                item.setTotalValue(totalValueForItem);

                items.add(item);
                totalValue = totalValue.add(totalValueForItem);
            }
        }

        response.setTotalProducts((long) items.size());
        response.setTotalValue(totalValue);
        response.setProducts(items);
        return response;
    }

    /**
     * 财务汇总报表
     */
    public ReportDtos.FinanceSummaryResponse getFinanceSummary(ReportDtos.FinanceSummaryRequest request) {
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1); // 默认本月1号
        }
        if (endDate == null) {
            endDate = LocalDate.now(); // 默认今天
        }

        ReportDtos.FinanceSummaryResponse response = new ReportDtos.FinanceSummaryResponse();
        response.setReportDate(endDate);

        // 计算销售额（从已审核的出库单统计）
        BigDecimal totalSales = outboundMapper.selectList(Wrappers.<SalesOutbound>lambdaQuery()
                        .eq(SalesOutbound::getStatus, "AUDITED")
                        .between(SalesOutbound::getBizDate, startDate, endDate))
                .stream()
                .map(SalesOutbound::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算采购额（从采购入库单统计，这里需要添加相应的查询）
        BigDecimal totalPurchases = purchaseInboundMapper.selectByDateRange(startDate.toString(), endDate.toString()).stream()
                .filter(doc -> "AUDITED".equals(doc.getStatus()))
                .flatMap(doc -> purchaseInboundItemMapper.selectList(Wrappers.<com.erp.module.purchase.entity.PurchaseInboundItem>lambdaQuery()
                        .eq(com.erp.module.purchase.entity.PurchaseInboundItem::getInboundId, doc.getId())).stream())
                .map(com.erp.module.purchase.entity.PurchaseInboundItem::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算应收账款余额
        BigDecimal totalReceivables = receivableMapper.selectList(Wrappers.<Receivable>lambdaQuery()
                        .ne(Receivable::getStatus, "SETTLED")
                        .between(Receivable::getBusinessDate, startDate, endDate))
                .stream()
                .map(Receivable::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算应付账款余额
        BigDecimal totalPayables = payableMapper.selectList(Wrappers.<Payable>lambdaQuery()
                        .ne(Payable::getStatus, "SETTLED")
                        .between(Payable::getBizDate, startDate, endDate))
                .stream().map(payable -> {
                    BigDecimal amount = payable.getAmount() == null ? BigDecimal.ZERO : payable.getAmount();
                    BigDecimal paid = payable.getPaidAmount() == null ? BigDecimal.ZERO : payable.getPaidAmount();
                    return amount.subtract(paid).max(BigDecimal.ZERO);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算库存总值
        BigDecimal totalInventory = getInventoryTotalValue(endDate);

        // 计算净利润
        BigDecimal netProfit = totalSales.subtract(totalPurchases);

        response.setTotalSales(totalSales);
        response.setTotalPurchases(totalPurchases);
        response.setTotalReceivables(totalReceivables);
        response.setTotalPayables(totalPayables);
        response.setTotalInventory(totalInventory);
        response.setNetProfit(netProfit);

        return response;
    }

    private boolean hasSalesperson(SalesOutbound outbound, Long salespersonId) {
        var order = salesOrderMapper.selectById(outbound.getOrderId());
        return order != null && salespersonId.equals(order.getSalespersonId());
    }

    private Map<Long, InventoryLedger> getStocksByWarehouse(Long warehouseId, LocalDate date) {
        return inventoryLedgerMapper.selectLatestByWarehouseAsOf(warehouseId, date).stream()
                .collect(Collectors.toMap(InventoryLedger::getProductId, ledger -> ledger,
                        (first, second) -> first, LinkedHashMap::new));
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 获取截至日期的库存总价值
     */
    private BigDecimal getInventoryTotalValue(LocalDate date) {
        List<Warehouse> warehouses = warehouseMapper.selectList(Wrappers.emptyWrapper());
        BigDecimal totalValue = BigDecimal.ZERO;
        for (Warehouse warehouse : warehouses) {
            totalValue = totalValue.add(inventoryLedgerMapper.selectLatestByWarehouseAsOf(warehouse.getId(), date).stream()
                    .map(ledger -> safe(ledger.getBalanceAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        return totalValue.setScale(2, RoundingMode.HALF_UP);
    }
}