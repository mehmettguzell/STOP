package com.stop.match_service.match.dto.response;

import com.stop.match_service.match.entity.Status;
import com.stop.match_service.match.entity.Visibility;

import java.time.LocalDateTime;
import java.util.UUID;

public record MatchResponse(
        UUID id,
        String title,
        String description,
        String location,
        LocalDateTime startTime,
        Visibility visibility,
        Status status,
        UUID organizerId,
        Integer capacity,
        Integer participantCount,
        LocalDateTime createdAt
) {
}
