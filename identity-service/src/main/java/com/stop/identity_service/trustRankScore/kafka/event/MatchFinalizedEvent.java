package com.stop.identity_service.trustRankScore.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MatchFinalizedEvent(
        UUID matchId,
        UUID organizerId,
        Integer homeScore,
        Integer awayScore,
        Map<UUID, String> teams,          // userId → "HOME" | "AWAY"
        Map<UUID, BigDecimal> avgScores,  // userId → peer rating ortalaması (1-5)
        Instant finalizedAt
) {}
