package com.stop.notification_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stop.notification_service.entity.Notification;
import com.stop.notification_service.entity.NotificationType;
import com.stop.notification_service.kafka.event.ParticipantLeftEvent;
import com.stop.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParticipantLeftEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "match.participant.left", groupId = "notification-service-group")
    public void consume(String payload) {
        ParticipantLeftEvent event;
        try {
            event = objectMapper.readValue(payload, ParticipantLeftEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize ParticipantLeftEvent: {}", payload, e);
            return;
        }

        log.info("Received match.participant.left. matchId={} userId={}", event.matchId(), event.userId());

        // Organizatörün kendisi ayrılıyorsa bildirim gönderme
        if (event.userId().equals(event.organizerId())) {
            return;
        }

        notificationService.save(Notification.builder()
                .userId(event.organizerId())
                .type(NotificationType.PARTICIPANT_LEFT)
                .title("Oyuncu Ayrıldı")
                .message("Maçından bir oyuncu ayrıldı.")
                .referenceId(event.matchId())
                .build());
    }
}
