package com.stop.identity_service.user.service;

import com.stop.identity_service.common.error.IdentityErrorCode;
import com.stop.identity_service.common.exception.BusinessException;
import com.stop.identity_service.user.entity.auth.RefreshToken;
import com.stop.identity_service.user.entity.user.User;
import com.stop.identity_service.user.repository.RefreshTokenRepository;
import com.stop.identity_service.config.jwt.TokenProvider;
import com.stop.identity_service.config.jwt.RefreshTokenGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenProvider tokenProvider;
    private final RefreshTokenGenerator refreshTokenGenerator;

    public String generateAccessToken(User user) {

        log.debug("Generating access token userId={}", user.getId());

        return tokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );
    }

    @Transactional
    public String createAndStoreRefreshToken(User user) {

        log.info("Creating refresh token userId={}", user.getId());
        revokeAllActiveRefreshTokensForUser(user.getId());

        String token =
                refreshTokenGenerator.generate();

        saveRefreshToken(user, hash(token));

        return token;
    }

    @Transactional
    public UUID validateAndRotateRefreshToken(String refreshToken) {
        log.info("Validating and rotating refresh token");

        RefreshToken storedToken =
                getValidRefreshToken(refreshToken);

        revokeToken(storedToken);

        log.info("Refresh token rotated userId={}",
                storedToken.getUserId());

        return storedToken.getUserId();
    }

    private RefreshToken getValidRefreshToken(String refreshToken) {

        String hash = hash(refreshToken);

        RefreshToken stored =
                refreshTokenRepository
                        .findByTokenHash(hash)
                        .orElseThrow(() -> {

                            log.warn("Refresh token not found");

                            return new BusinessException(
                                    IdentityErrorCode.INVALID_REFRESH_TOKEN
                            );
                        });

        validateTokenState(stored);

        return stored;
    }

    private void validateTokenState(RefreshToken token) {

        if (token.isRevoked()) {

            log.warn("Refresh token revoked userId={}",
                    token.getUserId());

            throw new BusinessException(
                    IdentityErrorCode.REFRESH_TOKEN_REVOKED
            );
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {

            log.warn("Refresh token expired userId={}",
                    token.getUserId());

            throw new BusinessException(
                    IdentityErrorCode.REFRESH_TOKEN_EXPIRED
            );
        }
    }

    private void revokeToken(RefreshToken token) {
        log.debug("Revoking refresh token userId={}", token.getUserId());

        token.setRevoked(true);

        refreshTokenRepository.save(token);
    }

    private void saveRefreshToken(User user, String hash) {
        log.debug("Persisting refresh token hash userId={}", user.getId());

        refreshTokenRepository.save(
                RefreshToken.builder()
                        .userId(user.getId())
                        .tokenHash(hash)
                        .expiresAt(
                                Instant.now()
                                        .plus(7, ChronoUnit.DAYS)
                        )
                        .revoked(false)
                        .createdAt(Instant.now())
                        .build()
        );
    }

    private void revokeAllActiveRefreshTokensForUser(UUID userId) {
        int revokedCount = refreshTokenRepository.revokeAllActiveByUserId(userId);
        if (revokedCount > 0) {
            log.info("Revoked existing refresh tokens userId={} count={}", userId, revokedCount);
        }
    }

    @Transactional
    public void revokeAllRefreshTokens(UUID userId) {
        log.debug("Removing all refresh tokens userId={}", userId);
        refreshTokenRepository.deleteByUserId(userId);
    }

    private String hash(String token) {

        return DigestUtils.sha256Hex(token);
    }
}