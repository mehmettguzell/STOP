package com.stop.notification_service.kafka.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MatchStartedEvent(
        UUID matchId,
        UUID organizerId,
        List<UUID> participantIds,
        Instant startedAt
) {}
