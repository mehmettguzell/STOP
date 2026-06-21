package com.stop.match_service.invitation.dto.response;

import com.stop.match_service.invitation.entity.InvitationStatus;

import java.util.UUID;

public record InvitationResponse(
        UUID matchId,
        UUID senderId,
        UUID receiverId,
        InvitationStatus status
) {
}
