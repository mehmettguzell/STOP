package com.stop.identity_service.trustRankScore.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TrustScoreOverrideRequest(

        @NotNull
        @DecimalMin("-10.0")
        @DecimalMax("10.0")
        BigDecimal delta,

        String reason
) {}
