package com.stop.identity_service.moderation.dto.response;

import com.stop.identity_service.moderation.entity.ReportStatus;

import java.time.Instant;
import java.util.UUID;

public record ReportResponse(
        UUID id,
        UUID reporterId,
        UUID targetUserId,
        UUID matchId,
        String reason,
        ReportStatus status,
        Instant createdAt
) {
}
