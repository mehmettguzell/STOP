package com.stop.communication_service.dto;

import java.util.List;
import java.util.UUID;

public record PollOptionResponse(
        UUID id,
        String text,
        int position,
        int voteCount,
        List<UUID> voterIds
) {}
