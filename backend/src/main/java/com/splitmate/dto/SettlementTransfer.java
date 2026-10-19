package com.splitmate.dto;

import java.math.BigDecimal;

/** 結算演算法的輸出：fromUserId 應付給 toUserId 多少錢 */
public record SettlementTransfer(Long fromUserId, Long toUserId, BigDecimal amount) {
}
