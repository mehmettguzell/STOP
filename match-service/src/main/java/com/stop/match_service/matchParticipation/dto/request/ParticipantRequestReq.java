package com.stop.match_service.matchParticipation.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ParticipantRequestReq(
        @NotNull(message = "Match ID cannot be null")
        UUID matchId
) {
}
