package com.stop.match_service.matchParticipation.dto.response;

import com.stop.match_service.matchParticipation.entity.WaitlistStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record WaitlistEntryResponse(
        UUID id,
        UUID matchId,
        UUID userId,
        WaitlistStatus status,
        LocalDateTime createdAt,
        Integer position
) {
}
