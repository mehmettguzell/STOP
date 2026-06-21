package com.stop.identity_service.passwordreset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank
        String token,

        @NotBlank
        @Size(min = 8, max = 128, message = "Şifre en az 8 karakter olmalıdır")
        String newPassword
) {}
