package com.stop.notification_service.kafka.event;

import java.time.Instant;
import java.util.UUID;

public record ParticipantJoinedEvent(
        UUID matchId,
        UUID userId,
        UUID organizerId,
        Instant joinedAt
) {}
