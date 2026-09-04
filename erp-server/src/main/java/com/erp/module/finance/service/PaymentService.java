package com.erp.module.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.module.finance.dto.PaymentDtos;
import com.erp.module.finance.entity.Payment;
import com.erp.module.finance.entity.PaymentAllocation;
import com.erp.module.finance.entity.Payable;
import com.erp.module.finance.mapper.PaymentAllocationMapper;
import com.erp.module.finance.mapper.PaymentMapper;
import com.erp.module.finance.mapper.PayableMapper;
import com.erp.module.masterdata.entity.Supplier;
import com.erp.module.masterdata.mapper.SupplierMapper;
import com.erp.module.system.TokenStore;
import com.erp.module.system.service.DocSequenceService;
import com.erp.module.system.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentMapper paymentMapper;
    private final PaymentAllocationMapper allocationMapper;
    private final PayableMapper payableMapper;
    private final SupplierMapper supplierMapper;
    private final DocSequenceService docSequenceService;
    private final OperationLogService operationLogService;

    @Transactional
    public Payment createDraft(PaymentDtos.PaymentCreateRequest request, TokenStore.LoginUser user) {
        Supplier supplier = supplierMapper.selectById(request.getSupplierId());
        if (supplier == null || supplier.getIsActive() == 0) {
            throw new BusinessException("供应商不存在或已停用");
        }
        BigDecimal amount = request.getAmount();
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2) {
            throw new BusinessException("付款金额必须大于0且最多2位小数");
        }
        LocalDate bizDate = request.getBizDate() == null ? LocalDate.now() : request.getBizDate();
        Payment payment = new Payment();
        String period = bizDate.format(DateTimeFormatter.BASIC_ISO_DATE);
        payment.setDocNo(docSequenceService.nextDocNo("PAY", "PAY", period));
        payment.setSupplierId(request.getSupplierId());
        payment.setBizDate(bizDate);
        payment.setAmount(amount);
        payment.setMethod(request.getMethod() == null ? "转账" : request.getMethod());
        payment.setBankAccount(request.getBankAccount() == null ? "" : request.getBankAccount());
        payment.setStatus("DRAFT");
        payment.setRemark(request.getRemark() == null ? "" : request.getRemark());
        payment.setCreatedBy(user.userId());
        paymentMapper.insert(payment);

        BigDecimal allocated = BigDecimal.ZERO;
        Set<Long> payableIds = new HashSet<>();
        if (request.getAllocations() != null) {
            for (PaymentDtos.PaymentCreateRequest.AllocationItem input : request.getAllocations()) {
                if (!payableIds.add(input.getPayableId())) {
                    throw new BusinessException("应付账款不能重复核销");
                }
                BigDecimal lineAmount = input.getAmount();
                if (lineAmount == null || lineAmount.signum() <= 0 || lineAmount.scale() > 2) {
                    throw new BusinessException("核销金额必须大于0且最多2位小数");
                }
                Payable payable = payableMapper.selectById(input.getPayableId());
                validatePayable(payable, request.getSupplierId(), lineAmount);
                allocated = allocated.add(lineAmount);
                if (allocated.compareTo(amount) > 0) {
                    throw new BusinessException("核销总额不能超过付款金额");
                }
                PaymentAllocation allocation = new PaymentAllocation();
                allocation.setPaymentId(payment.getId());
                allocation.setPayableId(payable.getId());
                allocation.setAmount(lineAmount);
                allocationMapper.insert(allocation);
            }
        }
        return payment;
    }

    @Transactional
    public Payment audit(Long id, TokenStore.LoginUser user, String ip) {
        Payment payment = paymentMapper.selectById(id);
        if (payment == null) throw new BusinessException("付款单不存在");
        if (paymentMapper.claimAudit(id, "AUDITED", user.userId()) == 0) {
            throw new BusinessException("付款单不存在或已审核");
        }
        List<PaymentAllocation> allocations = allocationMapper.getByPaymentId(id);
        BigDecimal total = allocations.stream().map(PaymentAllocation::getAmount)
                .map(value -> value == null ? BigDecimal.ZERO : value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount()) != 0) {
            throw new BusinessException("付款单必须全部核销,不能存在未分配金额");
        }
        for (PaymentAllocation allocation : allocations) {
            Payable payable = payableMapper.selectForUpdate(allocation.getPayableId());
            validatePayable(payable, payment.getSupplierId(), allocation.getAmount());
            if (payableMapper.updatePaidAmount(payable.getId(), allocation.getAmount()) == 0) {
                throw new BusinessException("应付账款余额不足: " + payable.getDocNo());
            }
        }
        operationLogService.record(user, "payment", "AUDIT", "PAYMENT", id, payment.getDocNo(),
                "{\"amount\":" + payment.getAmount() + ",\"allocated\":" + total + "}", ip);
        return payment;
    }

    public PageResult<PaymentDtos.PaymentListResponse> getPayments(PaymentDtos.PaymentListRequest params) {
        LambdaQueryWrapper<Payment> query = new LambdaQueryWrapper<Payment>()
                .eq(params.getSupplierId() != null, Payment::getSupplierId, params.getSupplierId())
                .eq(params.getStatus() != null && !params.getStatus().isBlank(), Payment::getStatus, params.getStatus())
                .ge(params.getStartDate() != null, Payment::getBizDate, params.getStartDate())
                .le(params.getEndDate() != null, Payment::getBizDate, params.getEndDate())
                .orderByDesc(Payment::getCreatedAt);
        long page = params.getPage() == null ? 1L : Math.max(params.getPage(), 1L);
        long size = params.getSize() == null ? 10L : Math.min(Math.max(params.getSize(), 1L), 500L);
        Page<Payment> result = paymentMapper.selectPage(new Page<>(page, size), query);
        List<Payment> payments = result.getRecords();
        return new PageResult<>(result.getTotal(), payments.stream().map(this::toResponse).toList());
    }

    private void validatePayable(Payable payable, Long supplierId, BigDecimal amount) {
        if (payable == null) throw new BusinessException("应付账款不存在");
        if (!supplierId.equals(payable.getSupplierId())) throw new BusinessException("核销应付不属于当前供应商");
        if (!"UNSETTLED".equals(payable.getStatus()) && !"PARTIAL".equals(payable.getStatus())) {
            throw new BusinessException("应付账款已结清");
        }
        BigDecimal remaining = (payable.getAmount() == null ? BigDecimal.ZERO : payable.getAmount())
                .subtract(payable.getPaidAmount() == null ? BigDecimal.ZERO : payable.getPaidAmount());
        if (amount.compareTo(remaining) > 0) throw new BusinessException("核销金额超过应付余额: " + payable.getDocNo());
    }

    private PaymentDtos.PaymentListResponse toResponse(Payment payment) {
        PaymentDtos.PaymentListResponse response = new PaymentDtos.PaymentListResponse();
        response.setId(payment.getId()); response.setDocNo(payment.getDocNo());
        response.setSupplierId(payment.getSupplierId());
        Supplier supplier = supplierMapper.selectById(payment.getSupplierId());
        response.setSupplierName(supplier == null ? "" : supplier.getName());
        response.setBizDate(payment.getBizDate()); response.setAmount(payment.getAmount());
        BigDecimal allocatedAmount = allocationMapper.getAllocatedAmount(payment.getId());
        response.setAllocatedAmount(allocatedAmount == null ? BigDecimal.ZERO : allocatedAmount);
        response.setStatus(payment.getStatus()); response.setMethod(payment.getMethod());
        response.setBankAccount(payment.getBankAccount()); response.setRemark(payment.getRemark());
        response.setCreatedAt(payment.getCreatedAt());
        return response;
    }
}
