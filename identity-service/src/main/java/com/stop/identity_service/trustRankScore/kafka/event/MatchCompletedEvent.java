package com.stop.identity_service.trustRankScore.kafka.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


public record MatchCompletedEvent(
        UUID matchId,
        UUID organizerId,
        List<UUID> participantIds,
        Instant completedAt
) {}
