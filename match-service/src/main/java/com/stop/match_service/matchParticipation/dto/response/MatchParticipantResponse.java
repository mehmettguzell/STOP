package com.stop.match_service.matchParticipation.dto.response;

import com.stop.match_service.matchParticipation.entity.ParticipantStatus;
import com.stop.match_service.matchParticipation.entity.TeamType;

import java.util.UUID;

public record MatchParticipantResponse(
        UUID participantId,
        UUID matchId,
        UUID userId,
        ParticipantStatus status,
        TeamType team,
        Float positionX,
        Float positionY
) {
}
