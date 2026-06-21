package com.stop.match_service.matchParticipation.dto.request;

import com.stop.match_service.matchParticipation.entity.TeamType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record TeamAssignmentReq(
        @NotEmpty @Valid List<PlayerTeamEntry> assignments
) {
    public record PlayerTeamEntry(
            @NotNull UUID userId,
            @NotNull TeamType team
    ) {}
}
