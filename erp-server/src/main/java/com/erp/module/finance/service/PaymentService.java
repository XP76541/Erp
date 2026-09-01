package com.erp.module.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.common.PageResult;
import com.erp.common.BusinessException;
import com.erp.module.finance.entity.Payment;
import com.erp.module.finance.entity.PaymentAllocation;
import com.erp.module.finance.entity.Receivable;
import com.erp.module.finance.mapper.PaymentMapper;
import com.erp.module.finance.mapper.PaymentAllocationMapper;
import com.erp.module.finance.mapper.ReceivableMapper;
import com.erp.module.system.entity.SysUser;
import com.erp.module.system.service.DocSequenceService;
import com.erp.module.system.service.OperationLogService;
import com.erp.module.system.TokenStore;
import com.erp.module.finance.dto.PaymentDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentMapper paymentMapper;
    private final PaymentAllocationMapper paymentAllocationMapper;
    private final ReceivableMapper receivableMapper;
    private final DocSequenceService docSequenceService;
    private final ReceivableService receivableService;
    private final OperationLogService operationLogService;

    @Transactional
    public Payment createDraft(PaymentDtos.PaymentCreateRequest request, TokenStore.LoginUser user) {
        // 验证收款金额
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("收款金额必须大于0");
        }

        // 创建收款单
        Payment payment = new Payment();
        payment.setDocNo(docSequenceService.nextDocNo("PAY", "PAY", LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))));
        payment.setCustomerId(request.getCustomerId());
        payment.setBusinessDate(request.getBusinessDate() != null ? request.getBusinessDate() : LocalDate.now());
        payment.setAmount(request.getAmount());
        payment.setAllocatedAmount(BigDecimal.ZERO);
        payment.setStatus("DRAFT");
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setRemark(request.getRemark());

        paymentMapper.insert(payment);

        // 如果有核销明细，保存核销关系
        if (request.getAllocations() != null && !request.getAllocations().isEmpty()) {
            saveAllocations(payment.getId(), request.getAllocations());
            updateAllocationAmount(payment.getId());
        }

        return payment;
    }

    @Transactional
    public Payment audit(Long id, TokenStore.LoginUser user, String ip) {
        // 抢占状态机:并发双击审核只有一次生效
        Payment payment = paymentMapper.selectById(id);
        if (payment == null) {
            throw new BusinessException("收款单不存在");
        }
        if (!"DRAFT".equals(payment.getStatus())) {
            throw new BusinessException("收款单不是草稿状态,无法审核");
        }

        // 原子抢占审核
        int updated = paymentMapper.updateStatus(id, "AUDITED");
        if (updated == 0) {
            throw new BusinessException("收款单已被他人审核");
        }

        // 更新审核字段
        payment.setStatus("AUDITED");
        payment.setUpdatedAt(LocalDateTime.now());
        paymentMapper.updateById(payment);

        // 记录操作日志
        String detail = "{\"amount\":" + payment.getAmount() + ",\"allocated\":" + payment.getAllocatedAmount() + "}";
        operationLogService.record(user, "payment", "AUDIT",
                "PAY", id, payment.getDocNo(), detail, ip);

        return payment;
    }

    public PageResult<PaymentDtos.PaymentListResponse> getPayments(PaymentDtos.PaymentListRequest params) {
        QueryWrapper<Payment> wrapper = new QueryWrapper<>();
        if (params.getCustomerId() != null) {
            wrapper.eq("customer_id", params.getCustomerId());
        }
        if (params.getStatus() != null) {
            wrapper.eq("status", params.getStatus());
        }
        if (params.getStartDate() != null) {
            wrapper.ge("business_date", params.getStartDate());
        }
        if (params.getEndDate() != null) {
            wrapper.le("business_date", params.getEndDate());
        }
        wrapper.orderByDesc("created_at");

        List<Payment> payments = paymentMapper.selectList(wrapper);
        List<PaymentDtos.PaymentListResponse> responses = payments.stream()
                .map(this::convertToListResponse)
                .collect(Collectors.toList());

        return new PageResult<>(responses, payments.size());
    }

    public List<PaymentDtos.ReceivableListResponse> getReceivablesByCustomer(Long customerId) {
        List<Receivable> receivables = receivableMapper.getUnsettledByCustomerId(customerId);
        return receivables.stream()
                .map(this::convertToReceivableResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    private void saveAllocations(Long paymentId, List<PaymentDtos.CreateRequest.AllocationItem> allocations) {
        for (PaymentDtos.CreateRequest.AllocationItem allocation : allocations) {
            // 检查核销金额
            Receivable receivable = receivableMapper.selectById(allocation.getReceivableId());
            if (receivable == null) {
                throw new BusinessException("应收账款不存在: " + allocation.getReceivableId());
            }
            if (allocation.getAllocatedAmount().compareTo(receivable.getRemainingAmount()) > 0) {
                throw new BusinessException("核销金额不能超过应收余额: " + receivable.getDocNo());
            }

            // 创建核销记录
            PaymentAllocation paymentAllocation = new PaymentAllocation();
            paymentAllocation.setPaymentId(paymentId);
            paymentAllocation.setReceivableId(allocation.getReceivableId());
            paymentAllocation.setAllocatedAmount(allocation.getAllocatedAmount());
            paymentAllocationMapper.insert(paymentAllocation);

            // 更新应收账款
            receivableMapper.updatePaidAmount(allocation.getReceivableId(), allocation.getAllocatedAmount());
        }
    }

    @Transactional
    private void updateAllocationAmount(Long paymentId) {
        BigDecimal allocatedAmount = paymentAllocationMapper.getAllocatedAmount(paymentId);
        Payment payment = paymentMapper.selectById(paymentId);
        payment.setAllocatedAmount(allocatedAmount);
        paymentMapper.updateById(payment);
    }

    private PaymentDtos.PaymentListResponse convertToListResponse(Payment payment) {
        PaymentDtos.PaymentListResponse response = new PaymentDtos.PaymentListResponse();
        response.setId(payment.getId());
        response.setDocNo(payment.getDocNo());
        response.setCustomerId(payment.getCustomerId());
        response.setCustomerName(payment.getCustomerName());
        response.setBusinessDate(payment.getBusinessDate());
        response.setAmount(payment.getAmount());
        response.setAllocatedAmount(payment.getAllocatedAmount());
        response.setStatus(payment.getStatus());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setCreatedAt(payment.getCreatedAt().toString());
        return response;
    }

    private PaymentDtos.ReceivableListResponse convertToReceivableResponse(Receivable receivable) {
        PaymentDtos.ReceivableListResponse response = new PaymentDtos.ReceivableListResponse();
        response.setId(receivable.getId());
        response.setDocNo(receivable.getDocNo());
        response.setOrderDocNo(receivable.getOrderDocNo());
        response.setCustomerId(receivable.getCustomerId());
        response.setCustomerName(receivable.getCustomerName());
        response.setBusinessDate(receivable.getBusinessDate());
        response.setDueDate(receivable.getDueDate());
        response.setAmount(receivable.getAmount());
        response.setPaidAmount(receivable.getPaidAmount());
        response.setRemainingAmount(receivable.getRemainingAmount());
        response.setStatus(receivable.getStatus());
        response.setDaysOverdue(receivable.getDaysOverdue());
        response.setAgingBucket(receivable.getAgingBucket());
        response.setCreatedAt(receivable.getCreatedAt().toString());
        return response;
    }
}