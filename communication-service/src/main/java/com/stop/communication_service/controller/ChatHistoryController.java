package com.stop.communication_service.controller;

import com.stop.communication_service.config.jwt.SecurityUtils;
import com.stop.communication_service.dto.ChatHistoryResponse;
import com.stop.communication_service.dto.ChatMessageResponse;
import com.stop.communication_service.dto.ConversationSummary;
import com.stop.communication_service.entity.MatchChatParticipant;
import com.stop.communication_service.repository.DirectChatRepository;
import com.stop.communication_service.repository.MatchChatParticipantRepository;
import com.stop.communication_service.service.ChatService;
import com.stop.communication_service.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final ChatService chatService;
    private final ConversationService conversationService;
    private final MatchChatParticipantRepository matchChatParticipantRepository;
    private final DirectChatRepository directChatRepository;

    /**
     * Cursor-based chat geçmişi.
     * İlk yükleme: before parametresi olmadan çağır → son 50 mesaj gelir.
     * Yukarı kaydır (daha fazla yükle): before=nextCursor ile çağır.
     * Hem MATCH hem DIRECT chat için çalışır.
     */
    @GetMapping("/{chatId}/history")
    public ResponseEntity<ChatHistoryResponse> getHistory(
            @PathVariable UUID chatId,
            @RequestParam(required = false) Instant before,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();

        // Erişim kontrolü: maç katılımcısı VEYA DM tarafı olmalı
        boolean isMatchParticipant = matchChatParticipantRepository.existsByMatchIdAndUserId(chatId, userId);
        boolean isDirectChatMember = !isMatchParticipant &&
                directChatRepository.findById(chatId)
                        .map(dc -> dc.getUser1Id().equals(userId) || dc.getUser2Id().equals(userId))
                        .orElse(false);

        if (!isMatchParticipant && !isDirectChatMember) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(chatService.getHistory(chatId, before, size));
    }

    /** Oturumdaki kullanıcının tüm konuşmaları — son mesaj + okunmamış sayısıyla. */
    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationSummary>> getConversations() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(conversationService.getConversations(userId));
    }

    /** Toplam okunmamış mesaj sayısı (tab badge). */
    @GetMapping("/unread-count")
    public ResponseEntity<Integer> getTotalUnread() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(conversationService.getTotalUnread(userId));
    }

    /** Belirtilen sohbeti okundu olarak işaretler. */
    @PostMapping("/{chatId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID chatId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        conversationService.markAsRead(userId, chatId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/conversations/{chatId}")
    public ResponseEntity<Void> hideConversation(@PathVariable UUID chatId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        conversationService.hideConversation(userId, chatId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable UUID messageId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        chatService.deleteMessage(messageId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/match/{matchId}/locked")
    public ResponseEntity<Boolean> isChatLocked(@PathVariable UUID matchId) {
        return ResponseEntity.ok(chatService.isChatLocked(matchId));
    }

    /**
     * Kullanıcıyı bir maç sohbetine katılımcı olarak kaydeder (upsert).
     * Frontend, maç chat ekranını açarken bu endpoint'i çağırır.
     * Böylece Kafka event'i kaçırılsa bile katılımcı kaydı oluşur.
     */
    @PostMapping("/match/{matchId}/join")
    public ResponseEntity<Void> joinMatchChat(@PathVariable UUID matchId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (!matchChatParticipantRepository.existsByMatchIdAndUserId(matchId, userId)) {
            matchChatParticipantRepository.save(
                    MatchChatParticipant.builder()
                            .matchId(matchId)
                            .userId(userId)
                            .joinedAt(Instant.now())
                            .build()
            );
        }
        return ResponseEntity.ok().build();
    }
}
