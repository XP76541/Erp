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
import com.erp.module.sales.entity.SalesOrder;
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
        if (startDate.isAfter(endDate)) {
            throw new com.erp.common.BusinessException("报表日期范围无效");
        // 在数据库侧限定已审核及销售员范围，避免先取全量数据后过滤造成越权。
        List<SalesOutbound> outbounds = request.getSalespersonId() == null
                ? outboundMapper.selectAuditedByDateRange(startDate, endDate)
                : outboundMapper.selectAuditedByDateRangeAndSalesperson(startDate, endDate, request.getSalespersonId());
        outbounds = outbounds.stream()
                .filter(outbound -> request.getCustomerId() == null || request.getCustomerId().equals(outbound.getCustomerId()))
                .toList();

        Map<Long, SalesOrder> ordersById = salesOrderMapper.selectBatchIds(outbounds.stream()
                        .map(SalesOutbound::getOrderId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(SalesOrder::getId, o -> o));
        Map<Long, Customer> customersById = customerMapper.selectBatchIds(outbounds.stream()
                        .map(SalesOutbound::getCustomerId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Customer::getId, c -> c));
        Map<Long, List<SalesOutboundItem>> itemsByOutboundId = outbounds.isEmpty() ? Collections.emptyMap()
                : outboundItemMapper.selectByOutboundIds(outbounds.stream().map(SalesOutbound::getId).toList())
                .stream().collect(Collectors.groupingBy(SalesOutboundItem::getOutboundId));

        Map<LocalDate, List<SalesOutbound>> dateGroup = outbounds.stream()
                .collect(Collectors.groupingBy(SalesOutbound::getBizDate));
        List<ReportDtos.SalesDailyReportResponse> responses = new ArrayList<>();
        for (LocalDate date : dateGroup.keySet().stream().sorted().toList()) {
            ReportDtos.SalesDailyReportResponse response = new ReportDtos.SalesDailyReportResponse();
            response.setReportDate(date);
            List<SalesOutbound> dayOutbounds = dateGroup.get(date);
            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal shippedAmount = BigDecimal.ZERO;
            List<ReportDtos.SalesDailyReportItem> items = new ArrayList<>();
            for (SalesOutbound outbound : dayOutbounds) {
                ReportDtos.SalesDailyReportItem item = new ReportDtos.SalesDailyReportItem();
                item.setDocNo(outbound.getDocNo()); item.setBusinessDate(outbound.getBizDate());
                BigDecimal amount = safe(outbound.getTotalAmount()); item.setAmount(amount); totalAmount = totalAmount.add(amount);
                Customer customer = customersById.get(outbound.getCustomerId());
                item.setCustomerName(customer == null ? "" : customer.getName());
                SalesOrder order = ordersById.get(outbound.getOrderId());
                item.setSalespersonName(order == null ? "" : String.valueOf(order.getSalespersonId()));
                BigDecimal shipped = itemsByOutboundId.getOrDefault(outbound.getId(), List.of()).stream()
                        .map(SalesOutboundItem::getAmount).map(ReportService::safe).reduce(BigDecimal.ZERO, BigDecimal::add);
                item.setShippedAmount(shipped); shippedAmount = shippedAmount.add(shipped);
                item.setStatus(outbound.getStatus()); items.add(item);
            }
            response.setTotalOrders((long) dayOutbounds.size()); response.setTotalAmount(totalAmount);
            response.setShippedAmount(shippedAmount); response.setOrders(items); responses.add(response);
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
                BigDecimal unitCost = request.isIncludeCost() && quantity.signum() != 0
                        ? safe(snapshot.getBalanceAmount()).divide(quantity, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;

                ReportDtos.InventorySummaryItem item = new ReportDtos.InventorySummaryItem();
                item.setProductId(productId);
                item.setProductName(product.getName());
                item.setProductSpec(product.getSpec() != null ? product.getSpec() : "");
                item.setWarehouseId(warehouse.getId());
                item.setWarehouseName(warehouse.getName());
                item.setQuantity(quantity);
                item.setUnitCost(unitCost);
                BigDecimal totalValueForItem = request.isIncludeCost() ? safe(snapshot.getBalanceAmount()) : BigDecimal.ZERO;
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
        if (startDate.isAfter(endDate)) {
            throw new com.erp.common.BusinessException("报表日期范围无效");
        }

        ReportDtos.FinanceSummaryResponse response = new ReportDtos.FinanceSummaryResponse();
        response.setReportDate(endDate);

        // 计算销售额（从已审核的出库单统计）
        List<SalesOutbound> salesOutbounds = request.getSalespersonId() == null
                ? outboundMapper.selectAuditedByDateRange(startDate, endDate)
                : outboundMapper.selectAuditedByDateRangeAndSalesperson(startDate, endDate, request.getSalespersonId());
        BigDecimal totalSales = salesOutbounds.stream()
                .map(SalesOutbound::getTotalAmount).map(ReportService::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算采购额（从采购入库单统计，这里需要添加相应的查询）
        List<com.erp.module.purchase.entity.PurchaseInbound> inbounds = purchaseInboundMapper.selectByDateRange(startDate.toString(), endDate.toString()).stream()
                .filter(doc -> "AUDITED".equals(doc.getStatus())).toList();
        BigDecimal totalPurchases = inbounds.stream()
                .flatMap(doc -> purchaseInboundItemMapper.selectList(Wrappers.<com.erp.module.purchase.entity.PurchaseInboundItem>lambdaQuery()
                        .eq(com.erp.module.purchase.entity.PurchaseInboundItem::getInboundId, doc.getId())).stream())
                .map(com.erp.module.purchase.entity.PurchaseInboundItem::getAmount).map(ReportService::safe)
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

        // 当前出库明细没有成本快照字段，不能用销售额减采购额冒充毛利。
        BigDecimal netProfit = null;

        response.setTotalSales(totalSales);
        response.setTotalPurchases(totalPurchases);
        response.setTotalReceivables(totalReceivables);
        response.setTotalPayables(totalPayables);
        response.setTotalInventory(totalInventory);
        response.setNetProfit(netProfit);
        response.setCostDataAvailable(false);

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