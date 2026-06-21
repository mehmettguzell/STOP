package com.stop.notification_service.dto;

import com.stop.notification_service.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String message,
        UUID referenceId,
        boolean isRead,
        Instant createdAt
) {}
