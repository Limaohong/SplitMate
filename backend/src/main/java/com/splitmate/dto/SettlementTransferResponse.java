package com.splitmate.dto;

import java.math.BigDecimal;

public record SettlementTransferResponse(
        Long fromUserId,
        String fromDisplayName,
        Long toUserId,
        String toDisplayName,
        BigDecimal amount) {
}
