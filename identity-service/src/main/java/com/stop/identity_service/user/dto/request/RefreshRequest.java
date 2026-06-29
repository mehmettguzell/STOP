package com.stop.identity_service.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshRequest(

        @NotBlank(message = "Refresh token cannot be empty")

        @Size(min = 32, max = 512,
                message = "Invalid refresh token format")

        String refreshToken
) {}
