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
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "找不到群組，或你不是該群組的成員"),
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "邀請連結無效"),
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "此電子郵件已被註冊"),
    // 分攤明細加總與支出金額不一致，代表新增支出的邏輯有 bug（練習時常見），結算無法進行
    SETTLEMENT_UNBALANCED(HttpStatus.INTERNAL_SERVER_ERROR, "帳目不平衡：所有成員淨額加總不為 0，請檢查支出的分攤金額"),
    // 練習項目的骨架方法會拋出此錯誤，實作完成後就不會再出現
    NOT_IMPLEMENTED(HttpStatus.NOT_IMPLEMENTED, "此功能尚未實作（練習項目，見 PRACTICE.md）"),
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
