package com.stop.communication_service;

import com.stop.communication_service.entity.ChatMessage;
import com.stop.communication_service.repository.ChatMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CommunicationServiceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Test
    void savesAndOrdersChatMessagesAgainstRealPostgres() {
        UUID matchId = UUID.randomUUID();

        chatMessageRepository.save(ChatMessage.builder()
                .matchId(matchId)
                .senderId(UUID.randomUUID())
                .content("first")
                .build());
        chatMessageRepository.save(ChatMessage.builder()
                .matchId(matchId)
                .senderId(UUID.randomUUID())
                .content("second")
                .build());

        assertThat(chatMessageRepository.findAllByMatchIdOrderBySentAtAsc(matchId))
                .extracting(ChatMessage::getContent)
                .containsExactly("first", "second");
    }
}
