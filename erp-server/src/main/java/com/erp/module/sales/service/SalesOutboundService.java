package com.erp.module.sales.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.module.masterdata.entity.Customer;
import com.erp.module.masterdata.entity.Warehouse;
import com.erp.module.masterdata.mapper.CustomerMapper;
import com.erp.module.masterdata.mapper.WarehouseMapper;
import com.erp.module.inventory.service.InventoryService;
import com.erp.module.finance.entity.Receivable;
import com.erp.module.finance.mapper.ReceivableMapper;
import com.erp.module.system.TokenStore;
import com.erp.module.system.service.DocSequenceService;
import com.erp.module.system.service.OperationLogService;
import com.erp.module.system.service.SystemAuthorizationService;
import com.erp.module.sales.entity.SalesOrder;
import com.erp.module.sales.entity.SalesOrderItem;
import com.erp.module.sales.entity.SalesOutbound;
import com.erp.module.sales.entity.SalesOutboundItem;
import com.erp.module.sales.mapper.SalesOrderMapper;
import com.erp.module.sales.mapper.SalesOrderItemMapper;
import com.erp.module.sales.mapper.SalesOutboundMapper;
import com.erp.module.sales.mapper.SalesOutboundItemMapper;
import com.erp.module.sales.dto.SalesOutboundDtos;
import com.erp.module.sales.dto.SalesOutboundDtos.CreateRequest;
import com.erp.module.sales.dto.SalesOutboundDtos.ItemInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 销售出库单(OUT201):草稿创建 + 审核
 * 审核 = 单一大事务:抢占状态机 → 库存出库 → 生成应收 → 更新订单状态 → 审计字段 → 操作日志
 * 任一步失败整体回滚
 */
@Service
public class SalesOutboundService {

    private static final DateTimeFormatter PERIOD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SalesOutboundMapper outboundMapper;
    private final SalesOutboundItemMapper outboundItemMapper;
    private final SalesOrderMapper orderMapper;
    private final SalesOrderItemMapper orderItemMapper;
    private final CustomerMapper customerMapper;
    private final WarehouseMapper warehouseMapper;
    private final InventoryService inventoryService;
    private final ReceivableMapper receivableMapper;
    private final DocSequenceService docSequenceService;
    private final OperationLogService operationLogService;
    private final SystemAuthorizationService authorizationService;

    public SalesOutboundService(SalesOutboundMapper outboundMapper,
                               SalesOutboundItemMapper outboundItemMapper,
                               SalesOrderMapper orderMapper,
                               SalesOrderItemMapper orderItemMapper,
                               CustomerMapper customerMapper,
                               WarehouseMapper warehouseMapper,
                               InventoryService inventoryService,
                               ReceivableMapper receivableMapper,
                               DocSequenceService docSequenceService,
                               OperationLogService operationLogService,
                               SystemAuthorizationService authorizationService) {
        this.outboundMapper = outboundMapper;
        this.outboundItemMapper = outboundItemMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.customerMapper = customerMapper;
        this.warehouseMapper = warehouseMapper;
        this.inventoryService = inventoryService;
        this.receivableMapper = receivableMapper;
        this.docSequenceService = docSequenceService;
        this.operationLogService = operationLogService;
        this.authorizationService = authorizationService;
    }

    /** 分页列表:按单号/客户/状态过滤 */
    public PageResult<SalesOutboundDtos.ListResponse> page(long page, long size, String keyword, String status, String customerId,
                                                           TokenStore.LoginUser user) {
        Long scope = authorizationService.salespersonScope(user);
        Page<SalesOutbound> result = outboundMapper.selectPage(new Page<>(page, size),
                Wrappers.<SalesOutbound>lambdaQuery()
                        .like(StringUtils.hasText(keyword), SalesOutbound::getDocNo, keyword)
                        .eq(StringUtils.hasText(customerId), SalesOutbound::getCustomerId, customerId)
                        .eq(StringUtils.hasText(status), SalesOutbound::getStatus, status)
                        .inSql(scope != null, SalesOutbound::getOrderId,
                                "SELECT id FROM sales_order WHERE salesperson_id = " + scope)
                        .orderByDesc(SalesOutbound::getId));

        List<SalesOutboundDtos.ListResponse> listResponses = result.getRecords().stream()
                .map(this::toListResponse)
                .toList();

        return new PageResult<>(result.getTotal(), listResponses);
    }

    /** 单据详情(主表+明细) */
    public SalesOutboundDtos.DetailResponse detail(Long id, TokenStore.LoginUser user) {
        SalesOutbound doc = requireOutbound(id);
        requireSalespersonAccess(doc, user);
        List<SalesOutboundItem> items = outboundItemMapper.selectByOutboundId(id);
        return new SalesOutboundDtos.DetailResponse(doc, items);
    }

    /** 从销售订单创建出库单 */
    @Transactional
    public SalesOutboundDtos.CreateFromOrderResponse createFromOrder(Long orderId, CreateRequest request, TokenStore.LoginUser user) {
        SalesOrder order = orderMapper.selectById(orderId);
        if (order == null || !"AUDITED".equals(order.getStatus())) {
            throw new BusinessException("销售订单不存在或未审核");
        }
        authorizationService.requireUnrestrictedOrSalesperson(user, order.getSalespersonId());

        Customer customer = customerMapper.selectById(order.getCustomerId());
        if (customer == null || customer.getIsActive() == 0) {
            throw new BusinessException("客户不存在或已停用");
        }

        Warehouse warehouse = warehouseMapper.selectById(request.getWarehouseId());
        if (warehouse == null || warehouse.getIsActive() == 0) {
            throw new BusinessException("仓库不存在或已停用");
        }

        // 验证发货数量不超过未发货数量
        List<SalesOrderItem> orderItems = orderItemMapper.selectByOrderId(orderId);
        for (ItemInput input : request.getItems()) {
            SalesOrderItem orderItem = orderItems.stream()
                    .filter(item -> item.getId().equals(input.getOrderItemId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("订单明细不存在: " + input.getOrderItemId()));

            BigDecimal alreadyShipped = orderItemMapper.selectShippedQtyByOrderItemId(input.getOrderItemId());
            BigDecimal remaining = orderItem.getQty().subtract(alreadyShipped != null ? alreadyShipped : BigDecimal.ZERO);

            if (input.getQty().compareTo(remaining) > 0) {
                throw new BusinessException("发货数量超过未发货数量: 订单明细 " + input.getOrderItemId());
            }
        }

        // 创建出库单
        SalesOutbound outbound = new SalesOutbound();
        outbound.setDocNo(docSequenceService.nextDocNo("OUT", "OUT", LocalDate.now().format(PERIOD)));
        outbound.setOrderId(orderId);
        outbound.setCustomerId(order.getCustomerId());
        outbound.setWarehouseId(request.getWarehouseId());
        outbound.setBizDate(request.getBizDate() != null ? request.getBizDate() : LocalDate.now());
        outbound.setStatus("DRAFT");
        outbound.setRemark(request.getRemark() == null ? "" : request.getRemark());
        outbound.setCreatedBy(user.userId());
        outboundMapper.insert(outbound);

        // 创建出库明细
        int lineNo = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<SalesOutboundItem> outboundItems = new ArrayList<>();

        for (ItemInput input : request.getItems()) {
            lineNo++;
            SalesOrderItem orderItem = orderItems.stream()
                    .filter(item -> item.getId().equals(input.getOrderItemId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("订单明细不存在: " + input.getOrderItemId()));

            SalesOutboundItem outboundItem = new SalesOutboundItem();
            outboundItem.setOutboundId(outbound.getId());
            outboundItem.setOrderItemId(input.getOrderItemId());
            outboundItem.setLineNo(lineNo);
            outboundItem.setProductId(orderItem.getProductId());
            outboundItem.setQty(input.getQty());
            outboundItem.setPrice(orderItem.getPrice());
            outboundItem.setAmount(input.getQty().multiply(orderItem.getPrice()).setScale(2, RoundingMode.HALF_UP));
            outboundItem.setRemark(input.getRemark());
            outboundItem.setCreatedBy(user.userId());
            outboundItemMapper.insert(outboundItem);

            outboundItems.add(outboundItem);
            totalAmount = totalAmount.add(outboundItem.getAmount());
        }

        outbound.setTotalAmount(totalAmount);
        outboundMapper.updateById(outbound);

        SalesOutboundDtos.CreateFromOrderResponse response = new SalesOutboundDtos.CreateFromOrderResponse();
        response.setOutboundId(outbound.getId());
        response.setOutboundDocNo(outbound.getDocNo());
        response.setItems(outboundItems);
        return response;
    }

    /**
     * 审核 = 单一大事务:
     * ① 原子抢占 DRAFT→AUDITED ② 库存出库 ③ 生成应收 ④ 更新订单状态 ⑤ 审计字段 ⑥ 操作日志
     */
    @Transactional
    public void audit(Long id, TokenStore.LoginUser user, String ip) {
        // ① 抢占状态机:并发双击审核只有一次生效,失败者读到已审状态报错回滚
        if (outboundMapper.claimAudit(id, user.userId()) == 0) {
            throw new BusinessException("单据不存在或不是草稿状态,无法审核");
        }
        SalesOutbound outbound = requireOutbound(id);
        requireSalespersonAccess(outbound, user);

        // ② 库存出库:调用 InventoryService.stockOut
        List<SalesOutboundItem> items = outboundItemMapper.selectByOutboundId(id);
        for (SalesOutboundItem item : items) {
            // 更新订单发货数量
            orderItemMapper.updateShippedQty(item.getOrderItemId(), item.getLineNo(), item.getQty());

            // 库存出库
            inventoryService.stockOut("SALES_OUT", outbound.getId(), outbound.getDocNo(),
                    item.getProductId(), outbound.getWarehouseId(), item.getQty(),
                    outbound.getBizDate());
        }

        // ③ 生成应收
        Customer customer = customerMapper.selectById(outbound.getCustomerId());
        if (customer == null) {
            throw new BusinessException("客户信息不存在");
        }

        Receivable receivable = new Receivable();
        receivable.setDocNo(docSequenceService.nextDocNo("REC", "REC", LocalDate.now().format(PERIOD)));
        receivable.setOrderId(outbound.getOrderId());
        SalesOrder order = orderMapper.selectById(outbound.getOrderId());
        receivable.setOrderDocNo(order != null ? order.getDocNo() : "");
        receivable.setCustomerId(outbound.getCustomerId());
        receivable.setCustomerName(customer.getName());
        receivable.setBusinessDate(outbound.getBizDate());
        receivable.setDueDate(outbound.getBizDate().plusDays(customer.getPaymentTermDays() != null ? customer.getPaymentTermDays() : 0));
        receivable.setAmount(outbound.getTotalAmount());
        receivable.setPaidAmount(BigDecimal.ZERO);
        receivable.setRemainingAmount(outbound.getTotalAmount());
        receivable.setStatus("UNSETTLED");
        receivableMapper.insert(receivable);

        // ④ 更新订单发货状态
        updateOrderShipStatus(outbound.getOrderId());

        // ⑤ 审计字段
        outbound.setStatus("AUDITED");
        outbound.setAuditBy(user.userId());
        outbound.setAuditAt(LocalDateTime.now());
        outboundMapper.updateById(outbound);

        // ⑥ 操作日志(审计留痕)
        String detail = "{\"amount\":" + outbound.getTotalAmount() + ",\"lines\":" + items.size() + "}";
        operationLogService.record(user, "sales_outbound", "AUDIT",
                "OUT", id, outbound.getDocNo(), detail, ip);
    }

    /**
     * 驳回 = 单一大事务:
     * ① 原子抢占 DRAFT→VOID ② 审计字段 ③ 操作日志
     */
    @Transactional
    public void reject(Long id, TokenStore.LoginUser user, String ip) {
        // ① 抢占状态机:并发双击驳回只有一次生效
        if (outboundMapper.claimReject(id, user.userId()) == 0) {
            throw new BusinessException("单据不存在或不是草稿状态,无法驳回");
        }
        SalesOutbound outbound = requireOutbound(id);

        // ② 审计字段
        outbound.setStatus("VOID");
        outbound.setRejectBy(user.userId());
        outbound.setRejectAt(LocalDateTime.now());
        outboundMapper.updateById(outbound);

        // ③ 操作日志(审计留痕)
        List<SalesOutboundItem> items = outboundItemMapper.selectByOutboundId(id);
        String detail = "{\"amount\":" + outbound.getTotalAmount() + ",\"lines\":" + items.size() + "}";
        operationLogService.record(user, "sales_outbound", "REJECT",
                "OUT", id, outbound.getDocNo(), detail, ip);
    }

    /** 查询客户的出库单 */
    public List<SalesOutboundDtos.ListResponse> findByCustomerId(Long customerId, TokenStore.LoginUser user) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) throw new BusinessException("客户不存在");
        authorizationService.requireUnrestrictedOrSalesperson(user, customer.getSalespersonId());
        return outboundMapper.selectByCustomerId(customerId).stream()
                .map(this::toListResponse)
                .toList();
    }

    /** 查询指定仓库的出库单 */
    public List<SalesOutboundDtos.ListResponse> findByWarehouseId(Long warehouseId, TokenStore.LoginUser user) {
        Long scope = authorizationService.salespersonScope(user);
        return outboundMapper.selectByWarehouseId(warehouseId).stream()
                .filter(outbound -> scope == null || hasOrderScope(outbound, scope))
                .map(this::toListResponse)
                .toList();
    }

    /** 查询指定日期范围内的出库单 */
    public List<SalesOutboundDtos.ListResponse> findByDateRange(LocalDate startDate, LocalDate endDate,
                                                                  TokenStore.LoginUser user) {
        Long scope = authorizationService.salespersonScope(user);
        return outboundMapper.selectByDateRange(startDate, endDate).stream()
                .filter(outbound -> scope == null || hasOrderScope(outbound, scope))
                .map(this::toListResponse)
                .toList();
    }

    /** 查询待审核的出库单数量 */
    public int countDraftOutbounds() {
        return outboundMapper.countDraftOutbounds();
    }

    /** 查询已审核未收款的数量 */
    public int countUnpaidOutbounds() {
        return outboundMapper.countUnpaidOutbounds();
    }

    /** 更新订单发货状态 */
    private void updateOrderShipStatus(Long orderId) {
        List<SalesOrderItem> items = orderItemMapper.selectByOrderId(orderId);

        boolean allShipped = true;
        boolean anyShipped = false;

        for (SalesOrderItem item : items) {
            BigDecimal shippedQty = item.getShippedQty() != null ? item.getShippedQty() : BigDecimal.ZERO;
            if (shippedQty.compareTo(BigDecimal.ZERO) > 0) {
                anyShipped = true;
            }
            if (shippedQty.compareTo(item.getQty()) < 0) {
                allShipped = false;
            }
        }

        SalesOrder order = new SalesOrder();
        order.setId(orderId);
        if (allShipped) {
            order.setShipStatus("SHIPPED");
        } else if (anyShipped) {
            order.setShipStatus("PART_SHIPPED");
        }
        orderMapper.updateById(order);
    }

    private void requireSalespersonAccess(SalesOutbound outbound, TokenStore.LoginUser user) {
        SalesOrder order = orderMapper.selectById(outbound.getOrderId());
        if (order == null) throw new BusinessException("关联销售订单不存在");
        authorizationService.requireUnrestrictedOrSalesperson(user, order.getSalespersonId());
    }

    private boolean hasOrderScope(SalesOutbound outbound, Long salespersonId) {
        SalesOrder order = orderMapper.selectById(outbound.getOrderId());
        return order != null && salespersonId.equals(order.getSalespersonId());
    }

    private SalesOutbound requireOutbound(Long id) {
        SalesOutbound outbound = outboundMapper.selectById(id);
        if (outbound == null) {
            throw new BusinessException("销售出库单不存在");
        }
        return outbound;
    }

    private SalesOutboundDtos.ListResponse toListResponse(SalesOutbound outbound) {
        SalesOutboundDtos.ListResponse response = new SalesOutboundDtos.ListResponse();
        response.setId(outbound.getId());
        response.setDocNo(outbound.getDocNo());
        response.setOrderId(outbound.getOrderId());

        // 设置订单单号
        SalesOrder order = orderMapper.selectById(outbound.getOrderId());
        response.setOrderDocNo(order != null ? order.getDocNo() : "");

        response.setCustomerId(outbound.getCustomerId());

        // 设置客户名称
        Customer customer = customerMapper.selectById(outbound.getCustomerId());
        response.setCustomerName(customer != null ? customer.getName() : "");

        response.setWarehouseId(outbound.getWarehouseId());

        // 设置仓库名称
        Warehouse warehouse = warehouseMapper.selectById(outbound.getWarehouseId());
        response.setWarehouseName(warehouse != null ? warehouse.getName() : "");

        response.setStatus(outbound.getStatus());
        response.setTotalAmount(outbound.getTotalAmount());
        response.setBizDate(outbound.getBizDate());
        response.setCreatedAt(outbound.getCreatedAt());
        response.setUpdatedAt(outbound.getUpdatedAt());
        return response;
    }
}