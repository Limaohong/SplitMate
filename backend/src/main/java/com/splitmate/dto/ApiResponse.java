package com.splitmate.dto;

import com.splitmate.exception.ErrorCode;

/**
 * 統一回應格式。成功時 code 為 "OK"，失敗時為 {@link ErrorCode} 名稱，前端依 code 判斷而非依訊息文字。
 */
public record ApiResponse<T>(String code, String message, T data) {

    private static final String SUCCESS_CODE = "OK";

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(SUCCESS_CODE, "success", data);
    }

    public static ApiResponse<Void> failure(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.name(), message, null);
    }
}
