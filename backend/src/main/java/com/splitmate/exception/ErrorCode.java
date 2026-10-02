package com.splitmate.exception;

import org.springframework.http.HttpStatus;

/**
 * 錯誤碼集中定義：HTTP 狀態碼與預設訊息放在一起，避免散落在各處的魔術字串。
 */
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "請求參數驗證失敗"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "請求格式錯誤"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "請先登入"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "電子郵件或密碼錯誤"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "沒有權限執行此操作"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "找不到資源"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "找不到使用者"),
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "此電子郵件已被註冊"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "系統發生錯誤，請稍後再試");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
