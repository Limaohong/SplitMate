package com.splitmate.dto;

import java.util.List;

/**
 * 支出的 cursor 分頁結果（第二階段練習項目，規格見 practice-guide/11）。
 *
 * @param items      本頁支出，由新到舊
 * @param nextCursor 下一頁的 cursor（不透明字串，前端原樣帶回即可）；沒有下一頁時為 null
 */
public record ExpensePageResponse(List<ExpenseResponse> items, String nextCursor) {
}
