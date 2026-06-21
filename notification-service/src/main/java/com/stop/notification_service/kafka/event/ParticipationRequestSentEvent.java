package com.stop.notification_service.kafka.event;

import java.time.Instant;
import java.util.UUID;

public record ParticipationRequestSentEvent(
        UUID requestId,
        UUID matchId,
        UUID userId,
        UUID organizerId,
        String matchTitle,
        Instant sentAt
) {}
