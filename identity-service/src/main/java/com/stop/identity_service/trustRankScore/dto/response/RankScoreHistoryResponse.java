package com.stop.identity_service.trustRankScore.dto.response;

import com.stop.identity_service.trustRankScore.entity.RankScoreEventType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RankScoreHistoryResponse(
        UUID id,
        RankScoreEventType eventType,
        BigDecimal delta,
        BigDecimal scoreAfter,
        UUID sourceId,
        String sourceType,
        Instant createdAt
) {}
