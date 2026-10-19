package com.splitmate.dto;

import com.splitmate.entity.ExpenseShare;
import java.math.BigDecimal;

public record ExpenseShareResponse(Long userId, String displayName, BigDecimal shareAmount) {

    public static ExpenseShareResponse from(ExpenseShare expenseShare) {
        return new ExpenseShareResponse(expenseShare.getUser().getId(), expenseShare.getUser().getDisplayName(),
                expenseShare.getShareAmount());
    }
}
