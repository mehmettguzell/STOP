package com.stop.identity_service.trustRankScore.dto.response;

import com.stop.identity_service.trustRankScore.entity.TrustScoreEventType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TrustScoreHistoryResponse(
        UUID id,
        TrustScoreEventType eventType,
        BigDecimal delta,
        BigDecimal scoreAfter,
        UUID sourceId,
        String sourceType,
        Instant createdAt
) {}
