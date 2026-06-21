package com.stop.identity_service.friendship.dto.request;


import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FriendRequest(
        @NotNull(message = "Alıcı ID boş olamaz")
        UUID receiverId
) {
}
