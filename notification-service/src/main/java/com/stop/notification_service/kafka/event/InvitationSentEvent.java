package com.stop.notification_service.kafka.event;

import java.time.Instant;
import java.util.UUID;

public record InvitationSentEvent(
        UUID invitationId,
        UUID matchId,
        UUID senderId,
        UUID receiverId,
        Instant sentAt
) {}
