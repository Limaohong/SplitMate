package com.splitmate.dto;

import com.splitmate.entity.CurrencyCode;

/** 群組列表的單筆資料。由 JPQL constructor expression 直接查出，避免為了算人數載入所有成員 */
public record GroupSummaryResponse(Long id, String name, CurrencyCode currencyCode, long memberCount) {
}
