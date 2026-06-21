package com.stop.identity_service.friendship.kafka.event;

import java.time.Instant;
import java.util.UUID;

public record FriendRequestAcceptedEvent(
        UUID friendshipId,
        UUID requesterId,
        UUID acceptorId,
        Instant acceptedAt
) {}
