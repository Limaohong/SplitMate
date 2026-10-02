package com.splitmate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.splitmate.dto.AuthResponse;
import com.splitmate.dto.LoginRequest;
import com.splitmate.dto.RegisterRequest;
import com.splitmate.entity.User;
import com.splitmate.exception.BusinessException;
import com.splitmate.exception.ErrorCode;
import com.splitmate.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldNormalizeEmailWhenRegister() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenService.createAccessToken(any(User.class))).thenReturn("access-token");

        AuthResponse authResponse = authService.register(
                new RegisterRequest("  Alice@Example.COM ", "password123", " Alice "));

        assertThat(authResponse.user().email()).isEqualTo("alice@example.com");
        assertThat(authResponse.user().displayName()).isEqualTo("Alice");
        assertThat(authResponse.accessToken()).isEqualTo("access-token");
    }

    @Test
    void shouldRejectRegisteredEmailBeforeSaving() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("alice@example.com", "password123", "Alice")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED));
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void shouldTranslateConcurrentDuplicateInsertToBusinessException() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("uk_users_email"));

        assertThatThrownBy(() -> authService.register(new RegisterRequest("alice@example.com", "password123", "Alice")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED));
    }

    @Test
    void shouldReturnSameErrorForUnknownEmailAndWrongPassword() {
        User existingUser = new User("alice@example.com", "hashed-password", "Alice");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@example.com", "wrong-password")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "password123")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }
}
