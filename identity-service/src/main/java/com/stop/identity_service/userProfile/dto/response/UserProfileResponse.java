package com.stop.identity_service.userProfile.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UserProfileResponse(

        UUID userId,

        String displayName,

        String firstName,
        String lastName,

        LocalDate birthDate,

        String city,
        String position,

        Integer heightCm,
        Integer weightKg,

        String dominantFoot,

        String bio,
        String avatarUrl,

        BigDecimal trustScore,
        BigDecimal rankScore

) {}
