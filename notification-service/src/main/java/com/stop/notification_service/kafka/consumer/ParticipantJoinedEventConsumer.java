package com.stop.notification_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stop.notification_service.entity.Notification;
import com.stop.notification_service.entity.NotificationType;
import com.stop.notification_service.kafka.event.ParticipantJoinedEvent;
import com.stop.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParticipantJoinedEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "match.participant.joined", groupId = "notification-service-group")
    public void consume(String payload) {
        ParticipantJoinedEvent event;
        try {
            event = objectMapper.readValue(payload, ParticipantJoinedEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize ParticipantJoinedEvent: {}", payload, e);
            return;
        }

        log.info("Received match.participant.joined. matchId={} userId={} organizerId={}",
                event.matchId(), event.userId(), event.organizerId());

        // Organizatörün kendisi katılıyorsa bildirim gönderme
        if (event.organizerId() == null || event.userId().equals(event.organizerId())) {
            log.debug("Skipping notification: organizer joined own match or organizerId null. matchId={}", event.matchId());
            return;
        }

        notificationService.save(Notification.builder()
                .userId(event.organizerId())
                .type(NotificationType.PARTICIPANT_JOINED)
                .title("Yeni Katılımcı")
                .message("Maçına yeni bir oyuncu katıldı.")
                .referenceId(event.matchId())
                .build());
    }
}
