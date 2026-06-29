package com.stop.identity_service.friendship.dto.response;

import com.stop.identity_service.friendship.entity.FriendshipStatus;

import java.time.Instant;
import java.util.UUID;

public record FriendshipResponse(
        UUID id,
        UUID requesterId,
        UUID receiverId,
        FriendshipStatus status,
        Instant createdAt
) {}
