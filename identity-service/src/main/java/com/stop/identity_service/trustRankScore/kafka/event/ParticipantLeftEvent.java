package com.stop.identity_service.trustRankScore.kafka.event;

import java.time.Instant;
import java.util.UUID;


public record ParticipantLeftEvent(
        UUID matchId,
        UUID userId,
        Instant leftAt
) {}
