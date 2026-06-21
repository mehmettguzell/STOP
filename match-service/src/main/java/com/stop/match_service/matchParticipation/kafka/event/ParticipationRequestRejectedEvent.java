package com.stop.match_service.matchParticipation.kafka.event;

import java.time.Instant;
import java.util.UUID;

public record ParticipationRequestRejectedEvent(
        UUID requestId,
        UUID matchId,
        UUID userId,
        String matchTitle,
        Instant rejectedAt
) {}
