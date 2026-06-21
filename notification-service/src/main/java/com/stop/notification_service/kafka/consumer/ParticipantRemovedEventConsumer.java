package com.stop.notification_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stop.notification_service.entity.Notification;
import com.stop.notification_service.entity.NotificationType;
import com.stop.notification_service.kafka.event.ParticipantRemovedEvent;
import com.stop.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParticipantRemovedEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "match.participant.removed", groupId = "notification-service-group")
    public void consume(String payload) {
        ParticipantRemovedEvent event;
        try {
            event = objectMapper.readValue(payload, ParticipantRemovedEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize ParticipantRemovedEvent: {}", payload, e);
            return;
        }

        log.info("Received match.participant.removed. matchId={} userId={}", event.matchId(), event.userId());

        notificationService.save(Notification.builder()
                .userId(event.userId())
                .type(NotificationType.PARTICIPANT_REMOVED)
                .title("Maçtan Çıkarıldın")
                .message("Organizatör seni maçtan çıkardı.")
                .referenceId(event.matchId())
                .build());
    }
}
