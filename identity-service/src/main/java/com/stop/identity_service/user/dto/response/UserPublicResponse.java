package com.stop.identity_service.user.dto.response;

import java.math.BigDecimal;

public record UserPublicResponse(

        String displayName,
        BigDecimal trustScore,
        BigDecimal rankScore
) {
}
