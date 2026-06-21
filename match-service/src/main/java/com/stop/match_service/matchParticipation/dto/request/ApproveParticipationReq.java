package com.stop.match_service.matchParticipation.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ApproveParticipationReq(
        @NotNull(message = "Request ID cannot be null")
        UUID id
) {
}
