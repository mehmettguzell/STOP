package com.stop.match_service.match.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TransferOrganizerRequest(
        @NotNull
        UUID newOrganizerId
) {
}
