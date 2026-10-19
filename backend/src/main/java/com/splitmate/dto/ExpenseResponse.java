package com.splitmate.dto;

import com.splitmate.entity.Expense;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ExpenseResponse(
        Long id,
        String description,
        BigDecimal amount,
        Long payerUserId,
        String payerDisplayName,
        List<ExpenseShareResponse> shares,
        boolean hasReceipt,
        Instant createdAt) {

    /** 呼叫前 payer 與 shares 必須已載入（見 ExpenseRepository.findAllWithSharesByGroupId） */
    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getPayer().getId(),
                expense.getPayer().getDisplayName(),
                expense.getShares().stream().map(ExpenseShareResponse::from).toList(),
                expense.hasReceipt(),
                expense.getCreatedAt());
    }
}
