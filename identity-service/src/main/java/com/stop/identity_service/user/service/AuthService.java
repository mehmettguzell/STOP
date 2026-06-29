package com.stop.identity_service.user.service;

import com.stop.identity_service.common.error.IdentityErrorCode;
import com.stop.identity_service.common.exception.BusinessException;
import com.stop.identity_service.user.dto.request.LoginRequest;
import com.stop.identity_service.user.dto.request.RegisterRequest;
import com.stop.identity_service.user.dto.response.AuthResponse;
import com.stop.identity_service.user.entity.user.Role;
import com.stop.identity_service.user.entity.user.Status;
import com.stop.identity_service.user.entity.user.User;
import com.stop.identity_service.user.kafka.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;


    public AuthResponse login(LoginRequest request) {

        log.info("Login attempt");
        User user = userService.findOptionalByEmail(request.email())
                .orElseThrow(this::invalidCredentials);

        validateCredentials(request.password(), user);
        ensureAccountAllowedForSession(user);

        AuthResponse response = generateAuthTokens(user);

        log.info("Login successful userId={}", user.getId());

        return response;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        log.info("Register attempt email={}", request.email());

        if (userService.existsByEmail(request.email())) {
            log.warn("Register failed, email already exists email={}", request.email());
            throw new BusinessException(IdentityErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (userService.existsByDisplayName(request.displayName())) {
            log.warn("Register failed, displayName already exists displayName={}", request.displayName());
            throw new BusinessException(IdentityErrorCode.NAME_ALREADY_EXISTS);
        }

        if (userService.existsByPhoneNumber(request.phoneNumber())) {
            log.warn("Register failed, phone already exists phoneNumber={}", request.phoneNumber());
            throw new BusinessException(IdentityErrorCode.PHONE_ALREADY_EXISTS);
        }

        User savedUser = userService.registerUser(mapRegisterRequestToUserEntity(request));

        eventPublisher.publishEvent(new UserCreatedEvent(savedUser.getId(), Instant.now()));

        log.info("Register successful userId={}", savedUser.getId());

        return generateAuthTokens(savedUser);
    }

    public AuthResponse refresh(String refreshToken) {

        log.info("Refresh token request received");

        UUID userId = tokenService.validateAndRotateRefreshToken(refreshToken);

        User user = userService.findUserById(userId);
        ensureAccountAllowedForSession(user);

        AuthResponse response = generateAuthTokens(user);

        log.info("Refresh token rotation successful userId={}", userId);

        return response;
    }


    @Transactional
    public void logout(UUID userId) {
        log.info("Logout request userId={}", userId);
        userService.findUserById(userId);
        tokenService.revokeAllRefreshTokens(userId);
        log.info("Logout successful userId={}", userId);
    }

    // PRIVATE METHODS //

    private void validateCredentials(String rawPassword, User user) {

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw invalidCredentials();
        }
    }

    private void ensureAccountAllowedForSession(User user) {
        if (user.getStatus() == Status.DELETED) {
            log.warn("Session denied, user deleted userId={}", user.getId());
            throw new BusinessException(IdentityErrorCode.USER_DELETED);
        }
        if (user.getStatus() == Status.SUSPENDED) {
            log.warn("Session denied, user suspended userId={}", user.getId());
            throw new BusinessException(IdentityErrorCode.USER_SUSPENDED);
        }
    }

    private BusinessException invalidCredentials() {
        log.warn("Login failed due to invalid credentials");
        return new BusinessException(IdentityErrorCode.INVALID_CREDENTIALS);
    }

    private AuthResponse generateAuthTokens(User user) {

        String accessToken = tokenService.generateAccessToken(user);

        String refreshToken = tokenService.createAndStoreRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken);
    }

    private User mapRegisterRequestToUserEntity(RegisterRequest request) {
        return User.builder()
                .email(request.email())
                .phoneNumber(request.phoneNumber())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .role(Role.USER)
                .status(Status.ACTIVE)
                .trustScore(BigDecimal.valueOf(7.0))
                .rankScore(BigDecimal.valueOf(7.0))
                .build();
    }
}