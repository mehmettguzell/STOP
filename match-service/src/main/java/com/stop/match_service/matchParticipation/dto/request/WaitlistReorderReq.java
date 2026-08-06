package com.stop.match_service.matchParticipation.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record WaitlistReorderReq(
        @NotEmpty(message = "entryIds cannot be empty")
        List<UUID> entryIds
) {
}
