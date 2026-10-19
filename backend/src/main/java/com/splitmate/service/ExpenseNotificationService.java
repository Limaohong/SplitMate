package com.splitmate.service;

import com.splitmate.dto.ExpenseResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 透過 WebSocket（STOMP）通知群組成員（練習項目，規格見 PRACTICE.md「後端 3：即時通知」）。
 * 連線驗證與訂閱權限已由 StompAuthChannelInterceptor 處理，這裡只需要「送出訊息」。
 */
@Service
public class ExpenseNotificationService {

    private final SimpMessagingTemplate simpMessagingTemplate;

    public ExpenseNotificationService(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    /**
     * TODO 練習項目：把 ExpenseCreatedMessage 送到 WebSocketDestinations.getGroupExpensesTopic(groupId)。
     * 想一想：推播失敗（例如 broker 異常）時，應不應該讓「新增支出」整個失敗？
     */
    public void notifyExpenseCreated(Long groupId, ExpenseResponse expenseResponse) {
        // TODO 練習項目：使用 simpMessagingTemplate 送出訊息
    }
}
