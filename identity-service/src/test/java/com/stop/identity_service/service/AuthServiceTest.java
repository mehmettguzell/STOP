package com.stop.identity_service.service;

import com.stop.identity_service.user.service.AuthService;
import com.stop.identity_service.user.service.TokenService;
import com.stop.identity_service.common.error.IdentityErrorCode;
import com.stop.identity_service.common.exception.BusinessException;
import com.stop.identity_service.user.dto.request.LoginRequest;
import com.stop.identity_service.user.dto.response.AuthResponse;
import com.stop.identity_service.user.entity.user.Role;
import com.stop.identity_service.user.entity.user.Status;
import com.stop.identity_service.user.entity.user.User;
import com.stop.identity_service.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_userNotFound_returnsInvalidCredentials() {
        LoginRequest request = new LoginRequest("missing@example.com", "password123");

        when(userService.findOptionalByEmail(eq(request.email()))).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> authService.login(request)
        );

        assertEquals(
                IdentityErrorCode.INVALID_CREDENTIALS.getCode(),
                ex.getErrorCode().getCode()
        );

        verify(tokenService, never()).generateAccessToken(any());
    }

    @Test
    void login_wrongPassword_returnsInvalidCredentials() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .email("user@example.com")
                .passwordHash("stored-hash")
                .displayName("User")
                .role(Role.USER)
                .status(Status.ACTIVE)
                .trustScore(BigDecimal.valueOf(7.0))
                .rankScore(BigDecimal.valueOf(7.0))
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2024-01-02T00:00:00Z"))
                .build();

        LoginRequest request = new LoginRequest("user@example.com", "wrong-password");

        when(userService.findOptionalByEmail(eq(request.email()))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(eq(request.password()), eq("stored-hash"))).thenReturn(false);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> authService.login(request)
        );

        assertEquals(
                IdentityErrorCode.INVALID_CREDENTIALS.getCode(),
                ex.getErrorCode().getCode()
        );
    }

    @Test
    void login_success_generatesTokens() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .email("user@example.com")
                .passwordHash("stored-hash")
                .displayName("User")
                .role(Role.USER)
                .status(Status.ACTIVE)
                .trustScore(BigDecimal.valueOf(7.0))
                .rankScore(BigDecimal.valueOf(7.0))
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2024-01-02T00:00:00Z"))
                .build();

        LoginRequest request = new LoginRequest("user@example.com", "password123");

        when(userService.findOptionalByEmail(eq(request.email()))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(eq(request.password()), eq("stored-hash"))).thenReturn(true);
        when(tokenService.generateAccessToken(eq(user))).thenReturn("access-token");
        when(tokenService.createAndStoreRefreshToken(eq(user))).thenReturn("refresh-token");

        AuthResponse res = authService.login(request);

        assertEquals("access-token", res.accessToken());
        assertEquals("refresh-token", res.refreshToken());
    }

    @Test
    void login_suspendedUser_throwsUserSuspended() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .email("user@example.com")
                .passwordHash("stored-hash")
                .displayName("User")
                .role(Role.USER)
                .status(Status.SUSPENDED)
                .trustScore(BigDecimal.valueOf(7.0))
                .rankScore(BigDecimal.valueOf(7.0))
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2024-01-02T00:00:00Z"))
                .build();

        LoginRequest request = new LoginRequest("user@example.com", "password123");

        when(userService.findOptionalByEmail(eq(request.email()))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(eq(request.password()), eq("stored-hash"))).thenReturn(true);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> authService.login(request)
        );

        assertEquals(IdentityErrorCode.USER_SUSPENDED.getCode(), ex.getErrorCode().getCode());
        verify(tokenService, never()).generateAccessToken(any());
    }

    @Test
    void login_deletedUser_throwsUserDeleted() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .email("user@example.com")
                .passwordHash("stored-hash")
                .displayName("User")
                .role(Role.USER)
                .status(Status.DELETED)
                .trustScore(BigDecimal.valueOf(7.0))
                .rankScore(BigDecimal.valueOf(7.0))
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2024-01-02T00:00:00Z"))
                .build();

        LoginRequest request = new LoginRequest("user@example.com", "password123");

        when(userService.findOptionalByEmail(eq(request.email()))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(eq(request.password()), eq("stored-hash"))).thenReturn(true);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> authService.login(request)
        );

        assertEquals(IdentityErrorCode.USER_DELETED.getCode(), ex.getErrorCode().getCode());
        verify(tokenService, never()).generateAccessToken(any());
    }

    @Test
    void refresh_rotatesAndGeneratesTokens() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("user@example.com")
                .passwordHash("stored-hash")
                .displayName("User")
                .role(Role.USER)
                .status(Status.ACTIVE)
                .trustScore(BigDecimal.valueOf(7.0))
                .rankScore(BigDecimal.valueOf(7.0))
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2024-01-02T00:00:00Z"))
                .build();

        String refreshToken = "some-refresh-token";

        when(tokenService.validateAndRotateRefreshToken(eq(refreshToken))).thenReturn(userId);
        when(userService.findUserById(eq(userId))).thenReturn(user);
        when(tokenService.generateAccessToken(eq(user))).thenReturn("access-token");
        when(tokenService.createAndStoreRefreshToken(eq(user))).thenReturn("refresh-token");

        AuthResponse res = authService.refresh(refreshToken);

        assertEquals("access-token", res.accessToken());
        assertEquals("refresh-token", res.refreshToken());
    }

    @Test
    void refresh_suspendedUser_throwsUserSuspended() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("user@example.com")
                .passwordHash("stored-hash")
                .displayName("User")
                .role(Role.USER)
                .status(Status.SUSPENDED)
                .trustScore(BigDecimal.valueOf(7.0))
                .rankScore(BigDecimal.valueOf(7.0))
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2024-01-02T00:00:00Z"))
                .build();

        String refreshToken = "some-refresh-token";

        when(tokenService.validateAndRotateRefreshToken(eq(refreshToken))).thenReturn(userId);
        when(userService.findUserById(eq(userId))).thenReturn(user);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> authService.refresh(refreshToken)
        );

        assertEquals(IdentityErrorCode.USER_SUSPENDED.getCode(), ex.getErrorCode().getCode());
        verify(tokenService, never()).generateAccessToken(any());
    }
}

