package com.stop.communication_service.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PollResponse(
        UUID id,
        UUID chatId,
        UUID creatorId,
        String question,
        List<PollOptionResponse> options,
        int totalVotes,
        Instant createdAt,
        Instant closesAt
) {}
