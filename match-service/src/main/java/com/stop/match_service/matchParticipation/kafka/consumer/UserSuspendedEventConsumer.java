package com.stop.match_service.matchParticipation.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stop.match_service.matchParticipation.kafka.event.UserSuspendedEvent;
import com.stop.match_service.matchParticipation.service.UserEventCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserSuspendedEventConsumer {

    private final UserEventCleanupService userEventCleanupService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "identity.user.suspended", groupId = "match-service")
    public void handle(String payload) {
        try {
            UserSuspendedEvent event = objectMapper.readValue(payload, UserSuspendedEvent.class);
            userEventCleanupService.handleUserSuspended(event.userId());
        } catch (Exception e) {
            log.error("Failed to deserialize UserSuspendedEvent: {}", payload, e);
        }
    }
}
