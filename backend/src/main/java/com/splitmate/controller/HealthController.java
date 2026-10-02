package com.splitmate.controller;

import com.splitmate.dto.ApiResponse;
import com.splitmate.dto.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康檢查。後端啟動時 Flyway 必須先連上 DB 才能完成啟動，因此能回應即代表 DB 連線正常。
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final String HEALTHY_STATUS = "OK";

    @GetMapping
    public ApiResponse<HealthResponse> getHealth() {
        return ApiResponse.success(new HealthResponse(HEALTHY_STATUS));
    }
}
