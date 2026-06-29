package com.stop.communication_service.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID matchId,
        UUID senderId,
        String content,
        Instant sentAt,
        boolean deleted,
        String type,
        UUID pollId,
        PollResponse pollData
) {}
