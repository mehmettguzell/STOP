package com.stop.communication_service.service;

import com.stop.communication_service.dto.ChatHistoryResponse;
import com.stop.communication_service.dto.ChatMessageResponse;
import com.stop.communication_service.dto.PollResponse;
import com.stop.communication_service.entity.ChatMessage;
import com.stop.communication_service.entity.MatchChatLock;
import com.stop.communication_service.kafka.event.ChatMessageSentEvent;
import com.stop.communication_service.kafka.producer.ChatMessageProducer;
import com.stop.communication_service.repository.ChatMessageRepository;
import com.stop.communication_service.repository.DirectChatRepository;
import com.stop.communication_service.repository.HiddenConversationRepository;
import com.stop.communication_service.repository.MatchChatLockRepository;
import com.stop.communication_service.repository.MatchChatParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.PageRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Duration CHAT_LOCK_DELAY = Duration.ofHours(3);

    private final ChatMessageRepository repository;
    private final MatchChatLockRepository matchChatLockRepository;
    private final MatchChatParticipantRepository matchChatParticipantRepository;
    private final DirectChatRepository directChatRepository;
    private final HiddenConversationRepository hiddenRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageProducer chatMessageProducer;

    @Lazy
    @Autowired
    private PollService pollService;

    @Transactional
    public ChatMessageResponse saveAndBroadcast(UUID chatId, UUID senderId, String content, String chatType) {
        if ("MATCH".equals(chatType)) {
            matchChatLockRepository.findById(chatId).ifPresent(lock -> {
                if (Instant.now().isAfter(lock.getCompletedAt().plus(CHAT_LOCK_DELAY))) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CHAT_LOCKED");
                }
            });
        }

        ChatMessage saved = repository.save(
                ChatMessage.builder()
                        .matchId(chatId)
                        .senderId(senderId)
                        .content(content)
                        .build()
        );

        ChatMessageResponse response = toResponse(saved);
        messagingTemplate.convertAndSend("/topic/chat/" + chatId, response);

        List<UUID> participantIds = resolveParticipants(chatId, chatType);

        // Gizlenmiş konuşmayı tek sorguda toplu temizle, sonra inbox sinyali gönder
        hiddenRepository.deleteByUserIdInAndChatId(participantIds, chatId);
        participantIds.forEach(participantId ->
            messagingTemplate.convertAndSend("/topic/user/" + participantId + "/inbox-update", "")
        );

        chatMessageProducer.publish(new ChatMessageSentEvent(
                saved.getId(),
                chatId,
                senderId,
                chatType,
                participantIds,
                saved.getSentAt()
        ));

        return response;
    }

    @Transactional(readOnly = true)
    public ChatHistoryResponse getHistory(UUID chatId, Instant before, int size) {
        // size+1 çekiyoruz: eğer size+1 gelirse daha fazla mesaj var demek
        // before == null ise en son mesajlardan başlar, değilse cursor'dan öncesini getirir
        List<ChatMessage> fetched = (before == null)
                ? repository.findLatest(chatId, PageRequest.of(0, size + 1))
                : repository.findBefore(chatId, before, PageRequest.of(0, size + 1));

        boolean hasMore = fetched.size() > size;
        List<ChatMessage> page = hasMore ? fetched.subList(0, size) : fetched;

        // DB'den DESC geldi (en yeni önce), frontend için ASC'ye çevir (en eski üstte)
        List<ChatMessageResponse> messages = new ArrayList<>(page.stream().map(this::toResponse).toList());
        Collections.reverse(messages);

        // Bir sonraki sayfa için cursor: sayfadaki en eski mesajın sentAt'i
        Instant nextCursor = hasMore ? page.get(page.size() - 1).getSentAt() : null;

        return new ChatHistoryResponse(messages, nextCursor);
    }

    @Transactional
    public void deleteMessage(UUID messageId, UUID requesterId) {
        ChatMessage msg = repository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        if (!msg.getSenderId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your message");
        }
        if (msg.getDeletedAt() != null) return;
        msg.setDeletedAt(Instant.now());
        repository.save(msg);

        messagingTemplate.convertAndSend("/topic/chat/" + msg.getMatchId(), toResponse(msg));
    }

    @Cacheable(value = "chat:lock", key = "#matchId")
    public boolean isChatLocked(UUID matchId) {
        return matchChatLockRepository.findById(matchId)
                .map(lock -> Instant.now().isAfter(lock.getCompletedAt().plus(CHAT_LOCK_DELAY)))
                .orElse(false);
    }

    private List<UUID> resolveParticipants(UUID chatId, String chatType) {
        if ("DIRECT".equals(chatType)) {
            return directChatRepository.findById(chatId)
                    .map(dc -> List.of(dc.getUser1Id(), dc.getUser2Id()))
                    .orElse(List.of());
        }
        return matchChatParticipantRepository.findUserIdsByMatchId(chatId);
    }

    public ChatMessageResponse toResponse(ChatMessage msg) {
        boolean deleted = msg.getDeletedAt() != null;
        String type = msg.getType() != null ? msg.getType() : "TEXT";
        PollResponse pollData = null;
        if ("POLL".equals(type) && msg.getPollId() != null && !deleted) {
            try { pollData = pollService.toPollResponse(pollService.getPollEntity(msg.getPollId())); }
            catch (Exception ignored) {}
        }
        return new ChatMessageResponse(
                msg.getId(),
                msg.getMatchId(),
                msg.getSenderId(),
                deleted ? null : msg.getContent(),
                msg.getSentAt(),
                deleted,
                type,
                msg.getPollId(),
                pollData
        );
    }
}
