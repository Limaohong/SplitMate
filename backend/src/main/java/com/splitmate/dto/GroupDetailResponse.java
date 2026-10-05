package com.splitmate.dto;

import com.splitmate.entity.CurrencyCode;
import com.splitmate.entity.ExpenseGroup;
import java.time.Instant;
import java.util.List;

/** 群組詳細資料，只回給群組成員（含邀請碼） */
public record GroupDetailResponse(
        Long id,
        String name,
        CurrencyCode currencyCode,
        String inviteCode,
        Instant createdAt,
        List<GroupMemberResponse> members) {

    public static GroupDetailResponse of(ExpenseGroup group, List<GroupMemberResponse> members) {
        return new GroupDetailResponse(group.getId(), group.getName(), group.getCurrencyCode(),
                group.getInviteCode(), group.getCreatedAt(), members);
    }
}
