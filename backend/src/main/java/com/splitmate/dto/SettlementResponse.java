package com.splitmate.dto;

import com.splitmate.entity.CurrencyCode;
import java.util.List;

public record SettlementResponse(
        CurrencyCode currencyCode,
        List<MemberBalanceResponse> memberBalances,
        List<SettlementTransferResponse> transfers) {
}
