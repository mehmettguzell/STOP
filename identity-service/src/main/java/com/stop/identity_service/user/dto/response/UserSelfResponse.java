package com.stop.identity_service.user.dto.response;

import com.stop.identity_service.user.entity.user.Role;
import com.stop.identity_service.user.entity.user.Status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UserSelfResponse(

        UUID id,
        String email,
        String phoneNumber,
        String displayName,
        Role role,
        Status status,
        BigDecimal trustScore,
        BigDecimal rankScore,
        Instant createdAt,
        Instant updatedAt,
        Boolean phoneVerified,
        Boolean emailVerified

) {
}
