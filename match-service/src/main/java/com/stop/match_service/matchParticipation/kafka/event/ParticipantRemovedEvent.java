package com.stop.match_service.matchParticipation.kafka.event;

import java.time.Instant;
import java.util.UUID;

public record ParticipantRemovedEvent(
        UUID matchId,
        UUID userId,
        UUID removedBy,
        Instant removedAt
) {}
