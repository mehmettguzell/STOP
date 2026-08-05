package com.stop.notification_service.kafka.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JerseyColorDecidedEvent(
        UUID matchId,
        List<UUID> whiteTeamUserIds,
        List<UUID> blackTeamUserIds,
        Instant decidedAt
) {}
