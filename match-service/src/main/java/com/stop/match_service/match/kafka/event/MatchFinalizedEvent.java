package com.stop.match_service.match.kafka.event;

import com.stop.match_service.matchParticipation.entity.TeamType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MatchFinalizedEvent(
        UUID matchId,
        UUID organizerId,
        Integer homeScore,
        Integer awayScore,
        Map<UUID, TeamType> teams,        // userId → HOME/AWAY
        Map<UUID, BigDecimal> avgScores,  // userId → ortalama puan (1-5)
        Instant finalizedAt
) {}
