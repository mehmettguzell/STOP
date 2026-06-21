package com.stop.match_service.invitation.kafka.event;

import java.time.Instant;
import java.util.UUID;

public record InvitationSentEvent(
        UUID invitationId,
        UUID matchId,
        UUID senderId,
        UUID receiverId,
        Instant sentAt
) {}
