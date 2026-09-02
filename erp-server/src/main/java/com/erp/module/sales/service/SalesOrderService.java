package com.erp.module.sales.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.module.masterdata.entity.Customer;
import com.erp.module.masterdata.entity.Product;
import com.erp.module.masterdata.mapper.CustomerMapper;
import com.erp.module.masterdata.mapper.ProductMapper;
import com.erp.module.system.entity.SysUser;
import com.erp.module.system.mapper.SysUserMapper;
import com.erp.module.system.TokenStore;
import com.erp.module.system.service.DocSequenceService;
import com.erp.module.system.service.OperationLogService;
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
    private final DocSequenceService docSequenceService;
    private final OperationLogService operationLogService;

    public SalesOrderService(SalesOrderMapper orderMapper,
                            SalesOrderItemMapper itemMapper,
                            CustomerMapper customerMapper,
                            ProductMapper productMapper,
                            SysUserMapper userMapper,
                            DocSequenceService docSequenceService,
                            OperationLogService operationLogService) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.customerMapper = customerMapper;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
        this.docSequenceService = docSequenceService;
        this.operationLogService = operationLogService;
    }

    /** 分页列表:按单号/客户/状态过滤 */
    public PageResult<SalesOrderDtos.ListResponse> page(long page, long size, String keyword, String status, String customerId) {
        Page<SalesOrder> result = orderMapper.selectPage(new Page<>(page, size),
                Wrappers.<SalesOrder>lambdaQuery()
                        .like(StringUtils.hasText(keyword), SalesOrder::getDocNo, keyword)
                        .eq(StringUtils.hasText(customerId), SalesOrder::getCustomerId, customerId)
                        .eq(StringUtils.hasText(status), SalesOrder::getStatus, status)
                        .orderByDesc(SalesOrder::getId));

        List<SalesOrderDtos.ListResponse> listResponses = result.getRecords().stream()
                .map(this::toListResponse)
                .toList();

        return new PageResult<>(result.getTotal(), listResponses);
    }

    /** 单据详情(主表+明细) */
    public SalesOrderDtos.DetailResponse detail(Long id) {
        SalesOrder doc = requireDoc(id);
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
    public void audit(Long id, TokenStore.LoginUser user, String ip) {
        // ① 抢占状态机:并发双击审核只有一次生效,失败者读到已审状态报错回滚
        if (orderMapper.claimAudit(id, user.userId()) == 0) {
            throw new BusinessException("单据不存在或不是草稿状态,无法审核");
        }
        SalesOrder doc = requireDoc(id);

        // 更新审核字段
        doc.setStatus("AUDITED");
        doc.setAuditBy(user.userId());
        doc.setAuditAt(java.time.LocalDateTime.now());
        orderMapper.updateById(doc);

        // 操作日志(审计留痕)
        List<SalesOrderItem> items = itemMapper.selectList(
                Wrappers.<SalesOrderItem>lambdaQuery()
                        .eq(SalesOrderItem::getOrderId, id)
                        .orderByAsc(SalesOrderItem::getLineNo));
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
    public List<SalesOrderDtos.ListResponse> findByCustomerId(Long customerId) {
        return orderMapper.selectByCustomerId(customerId).stream()
                .map(this::toListResponse)
                .toList();
    }

    /** 查询销售人员的销售订单 */
    public List<SalesOrderDtos.ListResponse> findBySalespersonId(Long salespersonId) {
        return orderMapper.selectBySalespersonId(salespersonId).stream()
                .map(this::toListResponse)
                .toList();
    }

    /** 查询指定日期范围内的销售订单 */
    public List<SalesOrderDtos.ListResponse> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return orderMapper.selectByDateRange(startDate, endDate).stream()
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