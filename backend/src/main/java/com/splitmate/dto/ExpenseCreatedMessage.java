package com.splitmate.dto;

/**
 * 支出建立後透過 WebSocket 推播給群組成員的訊息（練習項目）。
 * 推播到 WebSocketDestinations.getGroupExpensesTopic(groupId)。
 */
public record ExpenseCreatedMessage(Long groupId, ExpenseResponse expense) {
}
