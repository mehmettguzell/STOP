package com.stop.communication_service.dto;

import java.util.UUID;

/**
 * POST /chat/direct endpoint'inin döndürdüğü yanıt.
 * chatId: WebSocket topic ve history için kullanılacak kanal kimliği.
 */
public record DirectChatResponse(
        UUID chatId,
        UUID otherUserId
) {}
