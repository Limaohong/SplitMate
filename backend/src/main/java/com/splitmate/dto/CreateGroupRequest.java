package com.splitmate.dto;

import com.splitmate.entity.CurrencyCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(
        @NotBlank @Size(max = 50) String name,
        // 不在 enum 內的值（例如 "ABC"）會在 JSON 反序列化時失敗，回傳 MALFORMED_REQUEST
        @NotNull CurrencyCode currencyCode) {
}
