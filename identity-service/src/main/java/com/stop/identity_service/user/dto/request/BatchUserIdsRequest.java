package com.stop.identity_service.user.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record BatchUserIdsRequest(
        @NotEmpty(message = "userIds is required")
        @Size(max = 200, message = "userIds must not exceed 200 entries")
        List<UUID> userIds
) {
}
