package com.stop.notification_service.kafka.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MatchCompletedEvent(
        UUID matchId,
        UUID organizerId,
        Integer homeScore,
        Integer awayScore,
        List<UUID> participantIds,
        Instant completedAt
) {}
