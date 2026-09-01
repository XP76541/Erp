package com.erp.module.finance.dto;

import com.erp.module.finance.entity.Payment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class PaymentDtos {
    public record CreateRequest(
            @NotNull(message = "供应商不能为空") Long supplierId,
            @NotNull(message = "业务日期不能为空") LocalDate bizDate,
            @NotNull(message = "付款金额不能为空") @DecimalMin(value = "0.01", message = "金额必须大于0") @Digits(integer = 16, fraction = 2) BigDecimal amount,
            @NotBlank(message = "付款方式不能为空") String method,
            String bankAccount,
            String remark,
            @Valid @NotEmpty(message = "核销明细不能为空") List<AllocationInput> allocations
    ) {
    }

    public record AllocationInput(
            @NotNull(message = "应付单ID不能为空") Long payableId,
            @NotNull(message = "核销金额不能为空") @DecimalMin(value = "0.01", message = "金额必须大于0") @Digits(integer = 16, fraction = 2) BigDecimal amount
    ) {
    }

    public record DetailResponse(Payment doc, List<AllocationItem> allocations) {
    }

    public record AllocationItem(Long id, Long paymentId, Long payableId, BigDecimal amount, String payableDocNo, BigDecimal outstandingAmount) {
    }
}