package com.stop.identity_service.moderation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ReportRequest(
        @NotNull(message = "Hedef kullanıcı seçilmelidir")
        UUID targetUserId,

        @NotNull(message = "Eşleşme ID'si boş olamaz")
        UUID matchId,

        @NotBlank(message = "Raporlama nedeni boş bırakılamaz")
        @Size(min = 10, max = 500, message = "Rapor nedeni 10 ile 500 karakter arasında olmalıdır")
        String reason
) {
}