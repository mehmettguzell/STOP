package com.stop.identity_service.friendship.dto.response;


import java.util.List;
import java.util.UUID;

public record FriendsResponse(
    List<UUID> friends
) {
}
