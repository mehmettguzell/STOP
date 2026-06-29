package com.stop.communication_service.dto;

import java.time.Instant;
import java.util.List;

/**
 * Cursor-based chat history response.
 * messages: ASC sıralı (en eski üstte), frontend doğrudan gösterebilir.
 * nextCursor: bir sonraki sayfayı çekmek için ?before= parametresi, null ise daha fazla mesaj yok.
 */
public record ChatHistoryResponse(
        List<ChatMessageResponse> messages,
        Instant nextCursor
) {}
