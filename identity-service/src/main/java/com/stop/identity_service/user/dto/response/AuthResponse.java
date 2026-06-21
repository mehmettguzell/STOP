package com.stop.identity_service.user.dto.response;

public record AuthResponse(
     String accessToken,
     String refreshToken
) {
}
