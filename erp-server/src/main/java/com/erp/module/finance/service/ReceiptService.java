package com.erp.module.finance.service;

import com.erp.common.BusinessException;
import com.erp.module.finance.dto.ReceivableDtos;
import com.erp.module.finance.entity.Receipt;
import com.erp.module.finance.entity.ReceiptAllocation;
import com.erp.module.finance.entity.Receivable;
import com.erp.module.finance.mapper.ReceiptAllocationMapper;
import com.erp.module.finance.mapper.ReceiptMapper;
import com.erp.module.finance.mapper.ReceivableMapper;
import com.erp.module.masterdata.entity.Customer;
import com.erp.module.masterdata.mapper.CustomerMapper;
import com.erp.module.system.TokenStore;
import com.erp.module.system.service.DocSequenceService;
import com.erp.module.system.service.OperationLogService;
import com.erp.module.system.service.SystemAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReceiptService {
    private final ReceiptMapper receiptMapper;
    private final ReceiptAllocationMapper allocationMapper;
    private final ReceivableMapper receivableMapper;
    private final CustomerMapper customerMapper;
    private final DocSequenceService docSequenceService;
    private final OperationLogService operationLogService;
    private final SystemAuthorizationService authorizationService;

    /** 将旧的单笔核销请求统一转换为客户收款模型。 */
    @Transactional
    public ReceivableDtos.ReceiptResponse createSingleAllocation(Long receivableId, BigDecimal amount,
                                                                  String method, String remark,
                                                                  TokenStore.LoginUser user) {
        Receivable receivable = receivableMapper.selectById(receivableId);
        if (receivable == null) throw new BusinessException("应收账款记录不存在");
        ReceivableDtos.ReceiptCreateRequest request = new ReceivableDtos.ReceiptCreateRequest();
        request.setCustomerId(receivable.getCustomerId());
        request.setBizDate(LocalDate.now());
        request.setAmount(amount);
        request.setMethod(method);
        request.setRemark(remark);
        ReceivableDtos.ReceiptCreateRequest.AllocationItem item =
                new ReceivableDtos.ReceiptCreateRequest.AllocationItem();
        item.setReceivableId(receivableId);
        item.setAmount(amount);
        request.setAllocations(List.of(item));
        return createAndAllocate(request, user);
    }

    /** 创建已审核收款单并完成多单核销；整个过程在一个事务中。 */
    @Transactional
    public ReceivableDtos.ReceiptResponse createAndAllocate(ReceivableDtos.ReceiptCreateRequest request,
                                                             TokenStore.LoginUser user) {
        validateAmount(request.getAmount(), "收款金额");
        if (request.getAllocations() == null || request.getAllocations().isEmpty()) {
            throw new BusinessException("至少需要一条应收核销明细");
        }
        String key = normalizeKey(request.getIdempotencyKey());
        if (key != null && key.length() > 100) throw new BusinessException("幂等键长度不能超过100个字符");
        String fingerprint = requestFingerprint(request);
        if (key != null) {
            Receipt existing = receiptMapper.selectByIdempotencyKey(key);
            if (existing != null) return existingResponseOrConflict(existing, fingerprint);
        }

        Customer customer = customerMapper.selectById(request.getCustomerId());
        if (customer == null || customer.getIsActive() == 0) throw new BusinessException("客户不存在或已停用");
        authorizationService.requireUnrestrictedOrSalesperson(user, customer.getSalespersonId());

        BigDecimal total = BigDecimal.ZERO;
        Set<Long> ids = new HashSet<>();
        List<ReceivableDtos.ReceiptCreateRequest.AllocationItem> allocations = new ArrayList<>(request.getAllocations());
        allocations.sort(Comparator.comparing(ReceivableDtos.ReceiptCreateRequest.AllocationItem::getReceivableId));
        for (ReceivableDtos.ReceiptCreateRequest.AllocationItem item : allocations) {
            validateAmount(item.getAmount(), "核销金额");
            if (!ids.add(item.getReceivableId())) throw new BusinessException("应收账款不能重复核销");
            Receivable receivable = receivableMapper.selectForUpdate(item.getReceivableId());
            if (receivable == null) throw new BusinessException("应收账款不存在");
            if (!request.getCustomerId().equals(receivable.getCustomerId())) throw new BusinessException("核销应收不属于当前客户");
            if (!"UNSETTLED".equals(receivable.getStatus()) && !"PARTIAL".equals(receivable.getStatus())) {
                throw new BusinessException("应收账款已结清: " + receivable.getDocNo());
            }
            if (item.getAmount().compareTo(nullSafe(receivable.getRemainingAmount())) > 0) {
                throw new BusinessException("核销金额超过应收余额: " + receivable.getDocNo());
            }
            total = total.add(item.getAmount());
        }
        if (total.compareTo(request.getAmount()) > 0) throw new BusinessException("核销总额不能超过收款金额");

        LocalDate bizDate = request.getBizDate() == null ? LocalDate.now() : request.getBizDate();
        Receipt receipt = new Receipt();
        receipt.setDocNo(docSequenceService.nextDocNo("RCV", "RCV", bizDate.toString()));
        receipt.setIdempotencyKey(key);
        receipt.setIdempotencyFingerprint(fingerprint);
        receipt.setCustomerId(customer.getId());
        receipt.setBizDate(bizDate);
        receipt.setAmount(request.getAmount());
        receipt.setMethod(request.getMethod() == null ? "转账" : request.getMethod());
        receipt.setBankAccount(request.getBankAccount() == null ? "" : request.getBankAccount());
        receipt.setStatus("AUDITED");
        receipt.setAuditBy(user.userId()); receipt.setAuditAt(LocalDateTime.now());
        receipt.setRemark(request.getRemark() == null ? "" : request.getRemark());
        receipt.setCreatedBy(user.userId()); receipt.setCreatedAt(LocalDateTime.now());
        try {
            receiptMapper.insert(receipt);
        } catch (DataIntegrityViolationException ex) {
            if (key == null) throw ex;
            // The insert failure marks the surrounding transaction rollback-only; do not query
            // or return from this doomed transaction. The caller can retry and then read the
            // committed receipt through the normal idempotency lookup path.
            throw new BusinessException(409, "幂等请求正在处理中，请稍后重试");
        }

        for (ReceivableDtos.ReceiptCreateRequest.AllocationItem item : allocations) {
            ReceiptAllocation allocation = new ReceiptAllocation();
            allocation.setReceiptId(receipt.getId()); allocation.setReceivableId(item.getReceivableId());
            allocation.setAmount(item.getAmount()); allocationMapper.insert(allocation);
            if (receivableMapper.updatePaidAmount(item.getReceivableId(), item.getAmount()) != 1) {
                throw new BusinessException("应收账款余额已变化，请刷新后重试");
            }
        }
        operationLogService.record(user, "receipt", "AUDIT", "RECEIPT", receipt.getId(), receipt.getDocNo(),
                "{\"amount\":" + request.getAmount() + ",\"allocated\":" + total + "}", null);
        return toResponse(receipt, total);
    }

    private void validateAmount(BigDecimal amount, String name) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2) throw new BusinessException(name + "必须大于0且最多2位小数");
    }
    private BigDecimal nullSafe(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private String money(BigDecimal value) {
        return nullSafe(value).setScale(2, java.math.RoundingMode.UNNECESSARY).toPlainString();
    }
    private String normalizeKey(String key) { return key == null || key.isBlank() ? null : key.trim(); }
    private ReceivableDtos.ReceiptResponse toResponse(Receipt r) { return toResponse(r, allocationMapper.getAllocatedAmount(r.getId())); }

    private ReceivableDtos.ReceiptResponse existingResponseOrConflict(Receipt existing, String fingerprint) {
        if (existing.getIdempotencyFingerprint() == null
                || !existing.getIdempotencyFingerprint().equals(fingerprint)) {
            throw new BusinessException(409, "幂等键已用于不同的收款请求，无法复用历史记录");
        }
        return toResponse(existing);
    }

    private String requestFingerprint(ReceivableDtos.ReceiptCreateRequest request) {
        LocalDate bizDate = request.getBizDate() == null ? LocalDate.now() : request.getBizDate();
        String method = request.getMethod() == null ? "转账" : request.getMethod();
        String bankAccount = request.getBankAccount() == null ? "" : request.getBankAccount();
        String remark = request.getRemark() == null ? "" : request.getRemark();
        StringBuilder canonical = new StringBuilder()
                .append(request.getCustomerId()).append('|')
                .append(bizDate).append('|')
                .append(money(request.getAmount())).append('|')
                .append(method).append('|')
                .append(bankAccount).append('|')
                .append(remark).append('|');
        request.getAllocations().stream()
                .sorted(java.util.Comparator.comparing(ReceivableDtos.ReceiptCreateRequest.AllocationItem::getReceivableId))
                .forEach(item -> canonical.append(item.getReceivableId()).append('=').append(money(item.getAmount())).append(';'));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256不可用", ex);
        }
    }

    private ReceivableDtos.ReceiptResponse toResponse(Receipt r, BigDecimal allocated) {
        ReceivableDtos.ReceiptResponse out = new ReceivableDtos.ReceiptResponse();
        out.setId(r.getId()); out.setDocNo(r.getDocNo()); out.setCustomerId(r.getCustomerId()); out.setBizDate(r.getBizDate());
        out.setAmount(r.getAmount()); out.setAllocatedAmount(allocated == null ? BigDecimal.ZERO : allocated);
        out.setStatus(r.getStatus()); out.setMethod(r.getMethod()); out.setBankAccount(r.getBankAccount()); out.setRemark(r.getRemark()); out.setCreatedAt(r.getCreatedAt());
        return out;
    }
}
