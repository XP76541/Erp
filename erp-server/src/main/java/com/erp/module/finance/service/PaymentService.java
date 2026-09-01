package com.erp.module.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.module.finance.dto.PaymentDtos;
import com.erp.module.finance.entity.Payment;
import com.erp.module.finance.entity.PaymentAllocation;
import com.erp.module.finance.entity.Payable;
import com.erp.module.finance.mapper.PaymentAllocationMapper;
import com.erp.module.finance.mapper.PaymentMapper;
import com.erp.module.finance.mapper.PayableMapper;
import com.erp.module.masterdata.entity.Supplier;
import com.erp.module.system.service.DocSequenceService;
import com.erp.module.system.service.OperationLogService;
import com.erp.module.system.TokenStore;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {
    private static final DateTimeFormatter PERIOD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PaymentMapper paymentMapper;
    private final PaymentAllocationMapper allocationMapper;
    private final PayableMapper payableMapper;
    private final DocSequenceService docSequenceService;
    private final OperationLogService operationLogService;
    private final com.erp.module.masterdata.mapper.SupplierMapper supplierMapper;

    public PaymentService(
            PaymentMapper paymentMapper,
            PaymentAllocationMapper allocationMapper,
            PayableMapper payableMapper,
            DocSequenceService docSequenceService,
            OperationLogService operationLogService,
            com.erp.module.masterdata.mapper.SupplierMapper supplierMapper) {
        this.paymentMapper = paymentMapper;
        this.allocationMapper = allocationMapper;
        this.payableMapper = payableMapper;
        this.docSequenceService = docSequenceService;
        this.operationLogService = operationLogService;
        this.supplierMapper = supplierMapper;
    }

    public IPage<Payment> page(int pageNum, int pageSize, String keyword) {
        QueryWrapper<Payment> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.like("doc_no", keyword).or().like("remark", keyword);
        }
        qw.orderByDesc("id");
        return paymentMapper.selectPage(new Page<>(pageNum, pageSize), qw);
    }

    public PaymentDtos.DetailResponse detail(Long id) {
        Payment doc = requireDoc(id);
        QueryWrapper<PaymentAllocation> qw = new QueryWrapper<PaymentAllocation>().eq("payment_id", id);
        List<PaymentAllocation> rows = allocationMapper.selectList(qw);
        List<Long> payableIds = rows.stream().map(PaymentAllocation::getPayableId).toList();

        Map<Long, Payable> payableMap = new HashMap<>();
        if (!payableIds.isEmpty()) {
            List<Payable> payables = payableMapper.selectBatchIds(payableIds);
            for (Payable p : payables) {
                payableMap.put(p.getId(), p);
            }
        }

        List<PaymentDtos.AllocationItem> items = new ArrayList<>();
        for (PaymentAllocation r : rows) {
            Payable p = payableMap.get(r.getPayableId());
            BigDecimal outstanding = p != null ? p.getAmount().subtract(p.getPaidAmount()) : BigDecimal.ZERO;
            items.add(new PaymentDtos.AllocationItem(r.getId(), r.getPaymentId(), r.getPayableId(), r.getAmount(),
                    p != null ? p.getDocNo() : "—", outstanding));
        }

        return new PaymentDtos.DetailResponse(doc, items);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(PaymentDtos.CreateRequest req, TokenStore.LoginUser user) {
        // 校验供应商存在且启用
        var supplier = supplierMapper.selectById(req.supplierId());
        if (supplier == null || supplier.getIsActive() == 0) {
            throw new IllegalArgumentException("供应商不存在或已停用");
        }

        // 核销金额总和校验
        BigDecimal totalAlloc = BigDecimal.ZERO;
        for (var alloc : req.allocations()) {
            totalAlloc = totalAlloc.add(alloc.amount());
        }
        if (totalAlloc.compareTo(req.amount()) != 0) {
            throw new IllegalArgumentException("核销明细总额(" + totalAlloc + ")与付款金额(" + req.amount() + ")不一致");
        }

        // 单号
        String docNo = docSequenceService.nextDocNo("PAY", "PAY", req.bizDate().format(PERIOD));

        // 插入付款单
        Payment doc = new Payment();
        doc.setDocNo(docNo);
        doc.setSupplierId(req.supplierId());
        doc.setBizDate(req.bizDate());
        doc.setAmount(req.amount());
        doc.setMethod(req.method());
        doc.setBankAccount(req.bankAccount() != null ? req.bankAccount() : "");
        doc.setStatus("DRAFT");
        doc.setCreatedBy(user.userId());
        paymentMapper.insert(doc);

        // 插入核销明细
        for (int i = 0; i < req.allocations().size(); i++) {
            var alloc = req.allocations().get(i);
            PaymentAllocation row = new PaymentAllocation();
            row.setPaymentId(doc.getId());
            row.setPayableId(alloc.payableId());
            row.setAmount(alloc.amount());
            allocationMapper.insert(row);
        }

        return doc.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, TokenStore.LoginUser user, String ip) {
        Payment doc = requireDoc(id);
        int updated = paymentMapper.claimAudit(id, user.userId());
        if (updated == 0) {
            throw new IllegalStateException("单据不存在或不是草稿状态，无法审核");
        }

        // 获取核销明细
        QueryWrapper<PaymentAllocation> qw = new QueryWrapper<PaymentAllocation>().eq("payment_id", id);
        List<PaymentAllocation> rows = allocationMapper.selectList(qw);

        // 逐行核销应付
        for (PaymentAllocation r : rows) {
            Payable p = payableMapper.selectForUpdate(r.getPayableId());
            if (p == null) {
                throw new IllegalArgumentException("应付单不存在(ID=" + r.getPayableId() + ")");
            }
            BigDecimal newPaid = p.getPaidAmount().add(r.getAmount());
            if (newPaid.compareTo(p.getAmount()) > 0) {
                throw new IllegalArgumentException("核销金额(" + r.getAmount() + ")超过应付单 " + p.getDocNo() + " 的未核销余额");
            }
            p.setPaidAmount(newPaid);
            if (newPaid.compareTo(p.getAmount()) == 0) {
                p.setStatus("SETTLED");
            } else if (p.getPaidAmount().compareTo(BigDecimal.ZERO) == 0) {
                p.setStatus("PARTIAL");
            }
            payableMapper.updateById(p);
        }

        // 记录操作日志
        operationLogService.record(user, "payment", "AUDIT", "PAYMENT", doc.getId(), doc.getDocNo(),
                "付款核销，金额=" + doc.getAmount(), ip);
    }

    private Payment requireDoc(@NotNull Long id) {
        Payment doc = paymentMapper.selectById(id);
        if (doc == null) {
            throw new IllegalArgumentException("付款单不存在");
        }
        return doc;
    }
}