package com.stop.match_service.match.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CompleteMatchReq(
        @NotNull @Min(0) Integer homeScore,
        @NotNull @Min(0) Integer awayScore
) {}