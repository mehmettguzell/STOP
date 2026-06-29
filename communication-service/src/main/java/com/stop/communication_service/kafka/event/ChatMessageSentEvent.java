package com.stop.communication_service.kafka.event;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageSentEvent(
        UUID messageId,
        UUID chatId,
        UUID senderId,
        String chatType,
        java.util.List<UUID> participantIds,
        Instant sentAt
) {}
