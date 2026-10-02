package com.splitmate.dto;

/**
 * 登入 / 註冊成功的回應。expiresInSeconds 讓前端知道 token 何時過期。
 */
public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds, UserResponse user) {
}
