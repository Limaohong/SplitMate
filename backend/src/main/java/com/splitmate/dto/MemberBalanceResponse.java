package com.splitmate.dto;

import java.math.BigDecimal;

/**
 * 成員的收支彙總。netAmount = paidAmount − owedAmount：正數代表應收回，負數代表應付出。
 */
public record MemberBalanceResponse(
        Long userId,
        String displayName,
        BigDecimal paidAmount,
        BigDecimal owedAmount,
        BigDecimal netAmount) {
}
