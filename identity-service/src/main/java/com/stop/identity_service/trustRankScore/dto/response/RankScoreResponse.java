package com.stop.identity_service.trustRankScore.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record RankScoreResponse(
        UUID userId,
        BigDecimal rankScore
) {}
