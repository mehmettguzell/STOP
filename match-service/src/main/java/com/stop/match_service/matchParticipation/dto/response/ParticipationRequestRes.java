package com.stop.match_service.matchParticipation.dto.response;

import com.stop.match_service.matchParticipation.entity.RequestStatus;

import java.util.UUID;

public record ParticipationRequestRes(
        UUID id,
        UUID userID,
        UUID matchId,
        RequestStatus status
) {
}
