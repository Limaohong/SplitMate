package com.splitmate.exception;

import com.splitmate.dto.ApiResponse;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return buildResponse(exception.getErrorCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException exception) {
        String fieldMessages = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return buildResponse(ErrorCode.VALIDATION_FAILED, fieldMessages);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedRequest(HttpMessageNotReadableException exception) {
        return buildResponse(ErrorCode.MALFORMED_REQUEST, ErrorCode.MALFORMED_REQUEST.getDefaultMessage());
    }

    /** Spring 6.1+ 對不存在的路徑拋出此例外；若不處理會落入下方的 Exception 而變成 500。 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException exception) {
        return buildResponse(ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND.getDefaultMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        // 未預期錯誤：完整 stack trace 只記在 log，不回傳給前端，避免洩漏內部實作
        LOGGER.error("Unexpected error", exception);
        return buildResponse(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage());
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.failure(errorCode, message));
    }
}
