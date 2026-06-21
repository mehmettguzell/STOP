package com.stop.communication_service.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * GET /chat/conversations liste öğesi.
 * type: "MATCH" | "DIRECT"
 * chatId: WebSocket topic ve history için kanal kimliği (matchId veya directChat.id)
 * referenceId: MATCH için matchId, DIRECT için otherUserId
 * title: görüntülenecek başlık (frontend tarafından doldurulabilir ya da backend'den gelebilir)
 */
public record ConversationSummary(
        UUID    chatId,
        String  type,           // "MATCH" | "DIRECT"
        UUID    referenceId,    // matchId veya otherUserId
        String  lastMessage,    // null olabilir (hiç mesaj yoksa)
        Instant lastMessageAt,  // null olabilir
        int     unreadCount
) {}
