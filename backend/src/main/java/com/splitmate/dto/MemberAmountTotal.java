package com.splitmate.dto;

import java.math.BigDecimal;

/** Repository 彙總查詢的投影結果：某位成員的金額加總（已付 / 應付） */
public record MemberAmountTotal(Long userId, BigDecimal totalAmount) {
}
