package com.stop.identity_service.friendship.kafka.event;

import java.time.Instant;
import java.util.UUID;

public record FriendRequestSentEvent(
        UUID friendshipId,
        UUID requesterId,
        UUID receiverId,
        Instant sentAt
) {}
