package com.splitmate.service;

import com.splitmate.dto.AuthResponse;
import com.splitmate.dto.LoginRequest;
import com.splitmate.dto.RegisterRequest;
import com.splitmate.dto.UserResponse;
import com.splitmate.entity.User;
import com.splitmate.exception.BusinessException;
import com.splitmate.exception.ErrorCode;
import com.splitmate.repository.UserRepository;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        String normalizedEmail = normalizeEmail(registerRequest.email());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        User newUser = new User(normalizedEmail, passwordEncoder.encode(registerRequest.password()),
                registerRequest.displayName().trim());
        try {
            // saveAndFlush：立即送出 INSERT，讓「兩個請求同時註冊同一 email」的唯一鍵衝突在這裡被捕捉
            return buildAuthResponse(userRepository.saveAndFlush(newUser));
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest loginRequest) {
        // 帳號不存在與密碼錯誤回傳相同錯誤，避免被用來探測哪些 email 已註冊
        User user = userRepository.findByEmail(normalizeEmail(loginRequest.email()))
                .filter(foundUser -> passwordEncoder.matches(loginRequest.password(), foundUser.getPasswordHash()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        return new AuthResponse(jwtTokenService.createAccessToken(user), TOKEN_TYPE,
                jwtTokenService.getAccessTokenTtlSeconds(), UserResponse.from(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
