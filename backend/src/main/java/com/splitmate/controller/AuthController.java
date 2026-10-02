package com.splitmate.controller;

import com.splitmate.dto.ApiResponse;
import com.splitmate.dto.AuthResponse;
import com.splitmate.dto.LoginRequest;
import com.splitmate.dto.RegisterRequest;
import com.splitmate.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登出不需要 API：JWT 是無狀態的，前端刪除 token 即完成登出（token 會在到期後自然失效）。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        return ApiResponse.success(authService.register(registerRequest));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ApiResponse.success(authService.login(loginRequest));
    }
}
