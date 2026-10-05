package com.splitmate.dto;

import com.splitmate.entity.CurrencyCode;

/**
 * 邀請連結預覽：讓受邀者在加入前先看到群組資訊。
 * 刻意不包含成員名單與邀請碼以外的細節，加入後才看得到。
 */
public record InvitationResponse(
        Long groupId,
        String groupName,
        CurrencyCode currencyCode,
        long memberCount,
        boolean isAlreadyMember) {
}
