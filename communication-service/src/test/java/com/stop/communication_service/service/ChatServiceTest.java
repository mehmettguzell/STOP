package com.stop.communication_service.service;

import com.stop.communication_service.dto.ChatHistoryResponse;
import com.stop.communication_service.dto.ChatMessageResponse;
import com.stop.communication_service.entity.ChatMessage;
import com.stop.communication_service.kafka.producer.ChatMessageProducer;
import com.stop.communication_service.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ChatMessageRepository repository;
    @Mock private MatchChatLockRepository matchChatLockRepository;
    @Mock private MatchChatParticipantRepository matchChatParticipantRepository;
    @Mock private DirectChatRepository directChatRepository;
    @Mock private HiddenConversationRepository hiddenRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private ChatMessageProducer chatMessageProducer;
    @Mock private PollService pollService;

    @InjectMocks
    private ChatService chatService;

    private UUID chatId;
    private UUID senderId;
    private UUID messageId;
    private ChatMessage savedMessage;

    @BeforeEach
    void setUp() {
        chatId = UUID.randomUUID();
        senderId = UUID.randomUUID();
        messageId = UUID.randomUUID();

        savedMessage = ChatMessage.builder()
                .id(messageId)
                .matchId(chatId)
                .senderId(senderId)
                .type("TEXT")
                .content("Merhaba!")
                .sentAt(Instant.now())
                .build();
    }

    // ==================== saveAndBroadcast ====================

    @Test
    void saveAndBroadcast_matchChat_savesAndBroadcasts() {
        when(matchChatLockRepository.findById(chatId)).thenReturn(Optional.empty());
        when(repository.save(any(ChatMessage.class))).thenReturn(savedMessage);
        when(matchChatParticipantRepository.findUserIdsByMatchId(chatId)).thenReturn(List.of(senderId));

        ChatMessageResponse response = chatService.saveAndBroadcast(chatId, senderId, "Merhaba!", "MATCH");

        assertThat(response).isNotNull();
        assertThat(response.matchId()).isEqualTo(chatId);
        assertThat(response.senderId()).isEqualTo(senderId);
        verify(repository).save(any(ChatMessage.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/" + chatId), any(ChatMessageResponse.class));
        verify(chatMessageProducer).publish(any());
    }

    @Test
    void saveAndBroadcast_directChat_resolvesDirectParticipants() {
        UUID user2 = UUID.randomUUID();
        when(repository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        com.stop.communication_service.entity.DirectChat directChat =
                com.stop.communication_service.entity.DirectChat.builder()
                        .id(chatId)
                        .user1Id(senderId)
                        .user2Id(user2)
                        .build();
        when(directChatRepository.findById(chatId)).thenReturn(Optional.of(directChat));

        ChatMessageResponse response = chatService.saveAndBroadcast(chatId, senderId, "Selam!", "DIRECT");

        assertThat(response).isNotNull();
        verify(hiddenRepository).deleteByUserIdInAndChatId(List.of(senderId, user2), chatId);
    }

    // ==================== getHistory ====================

    @Test
    void getHistory_withCursor_returnsMessagesInOrder() {
        Instant before = Instant.now();
        when(repository.findBefore(eq(chatId), eq(before), any())).thenReturn(List.of(savedMessage));

        ChatHistoryResponse result = chatService.getHistory(chatId, before, 20);

        assertThat(result.messages()).hasSize(1);
        assertThat(result.messages().get(0).id()).isEqualTo(messageId);
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void getHistory_noCursor_returnsLatestMessages() {
        when(repository.findLatest(eq(chatId), any())).thenReturn(List.of(savedMessage));

        ChatHistoryResponse result = chatService.getHistory(chatId, null, 20);

        assertThat(result.messages()).hasSize(1);
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void getHistory_empty_returnsEmptyList() {
        Instant before = Instant.now();
        when(repository.findBefore(eq(chatId), eq(before), any())).thenReturn(List.of());

        ChatHistoryResponse result = chatService.getHistory(chatId, before, 20);

        assertThat(result.messages()).isEmpty();
        assertThat(result.nextCursor()).isNull();
    }

    // ==================== deleteMessage ====================

    @Test
    void deleteMessage_happyPath_setsDeletedAt() {
        when(repository.findById(messageId)).thenReturn(Optional.of(savedMessage));
        when(repository.save(any())).thenReturn(savedMessage);

        chatService.deleteMessage(messageId, senderId);

        assertThat(savedMessage.getDeletedAt()).isNotNull();
        verify(repository).save(savedMessage);
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/" + chatId), any(ChatMessageResponse.class));
    }

    @Test
    void deleteMessage_notFound_throwsNotFound() {
        when(repository.findById(messageId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.deleteMessage(messageId, senderId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void deleteMessage_wrongSender_throwsForbidden() {
        when(repository.findById(messageId)).thenReturn(Optional.of(savedMessage));

        assertThatThrownBy(() -> chatService.deleteMessage(messageId, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void deleteMessage_alreadyDeleted_doesNothing() {
        savedMessage.setDeletedAt(Instant.now().minusSeconds(60));
        when(repository.findById(messageId)).thenReturn(Optional.of(savedMessage));

        chatService.deleteMessage(messageId, senderId);

        verify(repository, never()).save(any());
    }

    // ==================== isChatLocked ====================

    @Test
    void isChatLocked_noLock_returnsFalse() {
        when(matchChatLockRepository.findById(chatId)).thenReturn(Optional.empty());

        assertThat(chatService.isChatLocked(chatId)).isFalse();
    }

    // ==================== toResponse ====================

    @Test
    void toResponse_deletedMessage_returnsNullContent() {
        savedMessage.setDeletedAt(Instant.now());

        ChatMessageResponse response = chatService.toResponse(savedMessage);

        assertThat(response.deleted()).isTrue();
        assertThat(response.content()).isNull();
    }

    @Test
    void toResponse_normalMessage_returnsContent() {
        ChatMessageResponse response = chatService.toResponse(savedMessage);

        assertThat(response.deleted()).isFalse();
        assertThat(response.content()).isEqualTo("Merhaba!");
    }
}
