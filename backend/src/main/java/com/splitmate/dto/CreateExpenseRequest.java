package com.splitmate.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 新增支出的請求（練習項目，規格見 PRACTICE.md）。
 * 這裡的註解只做「格式」驗證；「付款人與分攤成員必須是群組成員」「分攤加總等於金額」等
 * 需要查資料庫或跨欄位的「業務規則」驗證，要在 ExpenseService 實作。
 */
public record CreateExpenseRequest(
        // 與資料庫 NUMERIC(12,2) 一致：整數最多 10 位、小數最多 2 位
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @NotBlank @Size(max = 100) String description,
        @NotNull Long payerUserId,
        // @Valid 讓清單內每一筆 ExpenseShareRequest 的註解也會被檢查
        @NotEmpty List<@Valid ExpenseShareRequest> shares) {
}
