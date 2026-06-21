package com.stop.match_service.match.kafka.event;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MatchUpdatedEvent(
        UUID matchId,
        UUID organizerId,
        List<UUID> participantIds,
        String title,
        String location,
        LocalDateTime startTime,
        List<String> changes,
        Instant updatedAt
) {}
