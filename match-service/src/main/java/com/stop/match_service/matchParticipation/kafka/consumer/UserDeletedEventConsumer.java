package com.stop.match_service.matchParticipation.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stop.match_service.matchParticipation.kafka.event.UserDeletedEvent;
import com.stop.match_service.matchParticipation.service.UserEventCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserDeletedEventConsumer {

    private final UserEventCleanupService userEventCleanupService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "identity.user.deleted", groupId = "match-service")
    public void handle(String payload) {
        try {
            UserDeletedEvent event = objectMapper.readValue(payload, UserDeletedEvent.class);
            userEventCleanupService.handleUserDeleted(event);
        } catch (Exception e) {
            log.error("Failed to deserialize UserDeletedEvent: {}", payload, e);
        }
    }
}
