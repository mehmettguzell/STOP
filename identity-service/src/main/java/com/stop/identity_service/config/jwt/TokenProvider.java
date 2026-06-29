package com.stop.identity_service.config.jwt;

import com.stop.identity_service.user.entity.user.Role;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class TokenProvider {
    private final JwtEncoder jwtEncoder;
    private static final long ACCESS_TOKEN_EXPIRATION_SECONDS = 900;

    public String generateAccessToken(UUID userId, String email, Role role) {
        log.debug("Generating JWT access token userId={} role={}", userId, role);
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId.toString())
                .claim("role", role.name())
                .claim("email", email)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ACCESS_TOKEN_EXPIRATION_SECONDS))
                .issuer("identity-service")
                .audience(List.of("stop-api"))
                .id(UUID.randomUUID().toString())
                .build();

        return jwtEncoder
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();
    }
}
