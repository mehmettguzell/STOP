package com.stop.identity_service.user.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record UserPublicResponse(

        UUID id,
        String displayName,
        BigDecimal trustScore,
        BigDecimal rankScore,
        String avatarUrl
) {
}
