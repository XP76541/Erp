package com.erp.module.sales.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.module.masterdata.entity.Customer;
import com.erp.module.masterdata.entity.Product;
import com.erp.module.masterdata.mapper.CustomerMapper;
import com.erp.module.masterdata.mapper.ProductMapper;
import com.erp.module.finance.mapper.ReceivableMapper;
import com.erp.module.system.entity.SysUser;
import com.erp.module.system.mapper.SysUserMapper;
import com.erp.module.system.TokenStore;
import com.erp.module.system.service.DocSequenceService;
import com.erp.module.system.service.OperationLogService;
import com.erp.module.system.service.SystemAuthorizationService;
import com.erp.module.sales.entity.SalesOrder;
import com.erp.module.sales.entity.SalesOrderItem;
import com.erp.module.sales.mapper.SalesOrderMapper;
import com.erp.module.sales.mapper.SalesOrderItemMapper;
import com.erp.module.sales.dto.SalesOrderDtos;
import com.erp.module.sales.dto.SalesOrderDtos.CreateRequest;
import com.erp.module.sales.dto.SalesOrderDtos.ItemInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 销售订单(S201/US-201):草稿创建 + 审核
 * 审核 = 单一大事务:抢占状态机 → 更新订单状态 → 审计字段 → 操作日志
 * 任一步失败整体回滚
 */
@Service
public class SalesOrderService {

    private static final DateTimeFormatter PERIOD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SalesOrderMapper orderMapper;
    private final SalesOrderItemMapper itemMapper;
    private final CustomerMapper customerMapper;
    private final ProductMapper productMapper;
    private final SysUserMapper userMapper;
    private final ReceivableMapper receivableMapper;
    private final DocSequenceService docSequenceService;
    private final OperationLogService operationLogService;
    private final SystemAuthorizationService authorizationService;

    public SalesOrderService(SalesOrderMapper orderMapper,
                            SalesOrderItemMapper itemMapper,
                            CustomerMapper customerMapper,
                            ProductMapper productMapper,
                            SysUserMapper userMapper,
                            ReceivableMapper receivableMapper,
                            DocSequenceService docSequenceService,
                            OperationLogService operationLogService,
                            SystemAuthorizationService authorizationService) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.customerMapper = customerMapper;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
        this.receivableMapper = receivableMapper;
        this.docSequenceService = docSequenceService;
        this.operationLogService = operationLogService;
        this.authorizationService = authorizationService;
    }

    /** 分页列表:按单号/客户/状态过滤 */
    public PageResult<SalesOrderDtos.ListResponse> page(long page, long size, String keyword, String status, String customerId,
                                                        TokenStore.LoginUser user) {
        Long scope = authorizationService.salespersonScope(user);
        Page<SalesOrder> result = orderMapper.selectPage(new Page<>(page, size),
                Wrappers.<SalesOrder>lambdaQuery()
                        .like(StringUtils.hasText(keyword), SalesOrder::getDocNo, keyword)
                        .eq(StringUtils.hasText(customerId), SalesOrder::getCustomerId, customerId)
                        .eq(StringUtils.hasText(status), SalesOrder::getStatus, status)
                        .eq(scope != null, SalesOrder::getSalespersonId, scope)
                        .orderByDesc(SalesOrder::getId));

        List<SalesOrderDtos.ListResponse> listResponses = result.getRecords().stream()
                .map(this::toListResponse)
                .toList();

        return new PageResult<>(result.getTotal(), listResponses);
    }

    /** 单据详情(主表+明细) */
    public SalesOrderDtos.DetailResponse detail(Long id, TokenStore.LoginUser user) {
        SalesOrder doc = requireDoc(id);
        authorizationService.requireUnrestrictedOrSalesperson(user, doc.getSalespersonId());
        List<SalesOrderItem> items = itemMapper.selectList(
                Wrappers.<SalesOrderItem>lambdaQuery()
                        .eq(SalesOrderItem::getOrderId, id)
                        .orderByAsc(SalesOrderItem::getLineNo));
        return new SalesOrderDtos.DetailResponse(doc, items);
    }

    /** 创建草稿:校验档案引用与状态,服务端计算金额 */
    @Transactional
    public Long create(CreateRequest request, TokenStore.LoginUser user) {
        Customer customer = customerMapper.selectById(request.getCustomerId());
        if (customer == null || customer.getIsActive() == 0) {
            throw new BusinessException("客户不存在或已停用");
        }
        SysUser salesperson = userMapper.selectById(request.getSalespersonId());
        if (salesperson == null || salesperson.getIsActive() == 0) {
            throw new BusinessException("销售人员不存在或已停用");
        }
        authorizationService.requireUnrestrictedOrSalesperson(user, request.getSalespersonId());
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("明细不能为空");
        }

        SalesOrder doc = new SalesOrder();
        doc.setDocNo(docSequenceService.nextDocNo("SO", "SO", LocalDate.now().format(PERIOD)));
        doc.setCustomerId(request.getCustomerId());
        doc.setSalespersonId(request.getSalespersonId());
        doc.setBizDate(request.getBizDate());
        doc.setStatus("DRAFT");
        doc.setShipStatus("UN_SHIPPED");
        doc.setRemark(request.getRemark() == null ? "" : request.getRemark());
        doc.setCreatedBy(user.userId());
        orderMapper.insert(doc);

        int lineNo = 0;
        for (ItemInput input : request.getItems()) {
            lineNo++;
            Product product = productMapper.selectById(input.getProductId());
            if (product == null || product.getIsActive() == 0) {
                throw new BusinessException("第 " + lineNo + " 行商品不存在或已停用");
            }
            SalesOrderItem item = new SalesOrderItem();
            item.setOrderId(doc.getId());
            item.setLineNo(lineNo);
            item.setProductId(input.getProductId());
            item.setQty(input.getQty());
            item.setPrice(input.getPrice());
            item.setAmount(input.getQty().multiply(input.getPrice()).setScale(2, RoundingMode.HALF_UP));
            item.setNote(input.getNote() == null ? "" : input.getNote());
            itemMapper.insert(item);
        }
        return doc.getId();
    }

    /**
     * 审核 = 单一大事务:
     * ① 原子抢占 DRAFT→AUDITED ② 审计字段 ③ 操作日志
     */
    @Transactional
    public void audit(Long id, TokenStore.LoginUser user, String ip, boolean forceConfirm) {
        // ① 权限校验必须先于状态机写入，避免越权请求改变状态
        SalesOrder existing = requireDoc(id);
        authorizationService.requireUnrestrictedOrSalesperson(user, existing.getSalespersonId());
        if (forceConfirm && !authorizationService.canForceSalesAudit(user)) {
            throw new BusinessException(403, "无权强制审核低于最低限价的订单");
        }
        List<SalesOrderItem> items = itemMapper.selectList(
                Wrappers.<SalesOrderItem>lambdaQuery()
                        .eq(SalesOrderItem::getOrderId, id)
                        .orderByAsc(SalesOrderItem::getLineNo));
        for (SalesOrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            BigDecimal minimum = product == null || product.getMinSalePrice() == null ? BigDecimal.ZERO : product.getMinSalePrice();
            if (minimum.compareTo(BigDecimal.ZERO) > 0 && item.getPrice().compareTo(minimum) < 0 && !forceConfirm) {
                throw new BusinessException("商品低于最低限价，需老板确认");
            }
        }
        // ② 抢占状态机:并发双击审核只有一次生效,失败者读到已审状态报错回滚
        if (orderMapper.claimAudit(id, user.userId()) == 0) {
            throw new BusinessException("单据不存在或不是草稿状态,无法审核");
        }
        SalesOrder doc = requireDoc(id);
        authorizationService.requireUnrestrictedOrSalesperson(user, doc.getSalespersonId());

        String detail = "{\"amount\":" + totalAmount(items) + ",\"lines\":" + items.size() + "}";
        operationLogService.record(user, "sales_order", "AUDIT",
                "SO", id, doc.getDocNo(), detail, ip);
    }

    /**
     * 驳回 = 单一大事务:
     * ① 原子抢占 DRAFT→VOID ② 审计字段 ③ 操作日志
     */
    @Transactional
    public void reject(Long id, TokenStore.LoginUser user, String ip) {
        // 权限校验必须先于状态机写入
        SalesOrder existing = requireDoc(id);
        authorizationService.requireUnrestrictedOrSalesperson(user, existing.getSalespersonId());
        // ① 抢占状态机:并发双击驳回只有一次生效
        if (orderMapper.claimReject(id, user.userId()) == 0) {
            throw new BusinessException("单据不存在或不是草稿状态,无法驳回");
        }
        SalesOrder doc = requireDoc(id);

        // 更新驳回字段
        doc.setStatus("VOID");
        doc.setRejectBy(user.userId());
        doc.setRejectAt(java.time.LocalDateTime.now());
        orderMapper.updateById(doc);

        // 操作日志(审计留痕)
        List<SalesOrderItem> items = itemMapper.selectList(
                Wrappers.<SalesOrderItem>lambdaQuery()
                        .eq(SalesOrderItem::getOrderId, id)
                        .orderByAsc(SalesOrderItem::getLineNo));
        String detail = "{\"amount\":" + totalAmount(items) + ",\"lines\":" + items.size() + "}";
        operationLogService.record(user, "sales_order", "REJECT",
                "SO", id, doc.getDocNo(), detail, ip);
    }

    /** 查询客户的销售订单 */
    public List<SalesOrderDtos.ListResponse> findByCustomerId(Long customerId, TokenStore.LoginUser user) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) throw new BusinessException("客户不存在");
        authorizationService.requireUnrestrictedOrSalesperson(user, customer.getSalespersonId());
        return orderMapper.selectByCustomerId(customerId).stream()
                .map(this::toListResponse)
                .toList();
    }

    /** 查询销售人员的销售订单 */
    public List<SalesOrderDtos.ListResponse> findBySalespersonId(Long salespersonId, TokenStore.LoginUser user) {
        authorizationService.requireUnrestrictedOrSalesperson(user, salespersonId);
        return orderMapper.selectBySalespersonId(salespersonId).stream()
                .map(this::toListResponse)
                .toList();
    }

    /** 查询指定日期范围内的销售订单 */
    public List<SalesOrderDtos.ListResponse> findByDateRange(LocalDate startDate, LocalDate endDate,
                                                               TokenStore.LoginUser user) {
        Long scope = authorizationService.salespersonScope(user);
        List<SalesOrder> orders = orderMapper.selectByDateRange(startDate, endDate);
        return orders.stream()
                .filter(order -> scope == null || scope.equals(order.getSalespersonId()))
                .map(this::toListResponse)
                .toList();
    }

    /** 查询待审核的销售订单数量 */
    public int countDraftOrders() {
        return orderMapper.countDraftOrders();
    }

    /** 查询已审核未发货的销售订单数量 */
    public int countUnshippedOrders() {
        return orderMapper.countUnshippedOrders();
    }

    private BigDecimal totalAmount(List<SalesOrderItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (SalesOrderItem item : items) {
            total = total.add(item.getAmount());
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private SalesOrder requireDoc(Long id) {
        SalesOrder doc = orderMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException("销售订单不存在");
        }
        return doc;
    }

    private SalesOrderDtos.ListResponse toListResponse(SalesOrder doc) {
        SalesOrderDtos.ListResponse response = new SalesOrderDtos.ListResponse();
        response.setId(doc.getId());
        response.setDocNo(doc.getDocNo());
        response.setCustomerId(doc.getCustomerId());

        // 设置客户名称（实际应用中应该通过JOIN查询或缓存）
        Customer customer = customerMapper.selectById(doc.getCustomerId());
        response.setCustomerName(customer != null ? customer.getName() : "");

        response.setSalespersonId(doc.getSalespersonId());

        // 设置销售人员名称
        SysUser salesperson = userMapper.selectById(doc.getSalespersonId());
        response.setSalespersonName(salesperson != null ? salesperson.getRealName() : "");

        response.setStatus(doc.getStatus());
        response.setShipStatus(doc.getShipStatus());
        response.setTotalAmount(doc.getTotalAmount());
        response.setBizDate(doc.getBizDate());
        response.setCreatedAt(doc.getCreatedAt());
        response.setUpdatedAt(doc.getUpdatedAt());
        return response;
    }
}