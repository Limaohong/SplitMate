package com.splitmate.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** 新增支出時的單筆分攤：userId 這位成員應負擔 shareAmount */
public record ExpenseShareRequest(
        @NotNull Long userId,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal shareAmount) {
}
