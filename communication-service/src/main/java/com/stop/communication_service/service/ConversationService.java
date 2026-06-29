package com.stop.communication_service.service;

import com.stop.communication_service.dto.ConversationSummary;
import com.stop.communication_service.entity.ChatMessage;
import com.stop.communication_service.entity.ChatRead;
import com.stop.communication_service.entity.DirectChat;
import com.stop.communication_service.entity.MatchChatParticipant;
import com.stop.communication_service.entity.HiddenConversation;
import com.stop.communication_service.repository.ChatMessageRepository;
import com.stop.communication_service.repository.ChatReadRepository;
import com.stop.communication_service.repository.DirectChatRepository;
import com.stop.communication_service.repository.HiddenConversationRepository;
import com.stop.communication_service.repository.MatchChatParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final DirectChatRepository            directChatRepository;
    private final MatchChatParticipantRepository  matchParticipantRepository;
    private final ChatMessageRepository           messageRepository;
    private final ChatReadRepository              chatReadRepository;
    private final HiddenConversationRepository    hiddenRepository;

    @Transactional(readOnly = true)
    public List<ConversationSummary> getConversations(UUID userId) {
        List<ConversationSummary> result = new ArrayList<>();
        var hidden = hiddenRepository.findChatIdsByUserId(userId);

        // 1) Maç sohbetleri — Kafka ile beslenen katılım kaydından
        for (MatchChatParticipant p : matchParticipantRepository.findAllByUserId(userId)) {
            if (hidden.contains(p.getMatchId())) continue;
            result.add(buildSummary(userId, p.getMatchId(), "MATCH", p.getMatchId()));
        }

        // 2) Bire-bir sohbetler
        for (DirectChat dc : directChatRepository.findAllByUserId(userId)) {
            if (hidden.contains(dc.getId())) continue;
            UUID otherId = dc.getUser1Id().equals(userId) ? dc.getUser2Id() : dc.getUser1Id();
            result.add(buildSummary(userId, dc.getId(), "DIRECT", otherId));
        }

        // En son mesajı olan konuşma üstte; hiç mesajı olmayanlar en alta
        result.sort(Comparator.comparing(
                ConversationSummary::lastMessageAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        return result;
    }

    private ConversationSummary buildSummary(UUID userId, UUID chatId, String type, UUID referenceId) {
        // Son mesaj (önizleme metni + zaman)
        var lastMsgs    = messageRepository.findLastMessage(chatId, PageRequest.of(0, 1));
        ChatMessage lastMsg = lastMsgs.isEmpty() ? null : lastMsgs.get(0);
        String content = lastMsg != null ? lastMsg.getContent() : null;
        String  lastContent = (content != null && !content.isBlank()) ? content : null;
        Instant lastAt      = lastMsg != null ? lastMsg.getSentAt()  : null;

        // Okunmamış sayısı: son okunma zamanından sonra, başkası tarafından gönderilen
        Instant since = chatReadRepository
                .findByUserIdAndChatId(userId, chatId)
                .map(ChatRead::getLastReadAt)
                .orElse(Instant.EPOCH);   // Hiç okunmadıysa en baştan say

        int unread = (int) messageRepository.countUnread(chatId, since, userId);

        return new ConversationSummary(chatId, type, referenceId, lastContent, lastAt, unread);
    }

    @Transactional
    public void markAsRead(UUID userId, UUID chatId) {
        ChatRead read = chatReadRepository.findByUserIdAndChatId(userId, chatId)
                .orElseGet(() -> ChatRead.builder()
                        .userId(userId)
                        .chatId(chatId)
                        .lastReadAt(Instant.now())
                        .build());
        read.setLastReadAt(Instant.now());
        chatReadRepository.save(read);
    }

    @Transactional
    public void hideConversation(UUID userId, UUID chatId) {
        if (!hiddenRepository.existsByUserIdAndChatId(userId, chatId)) {
            hiddenRepository.save(HiddenConversation.builder()
                    .userId(userId)
                    .chatId(chatId)
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public int getTotalUnread(UUID userId) {
        return getConversations(userId).stream()
                .mapToInt(ConversationSummary::unreadCount)
                .sum();
    }
}
