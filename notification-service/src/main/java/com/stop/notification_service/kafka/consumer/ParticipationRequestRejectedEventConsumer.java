package com.stop.notification_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stop.notification_service.entity.Notification;
import com.stop.notification_service.entity.NotificationType;
import com.stop.notification_service.kafka.event.ParticipationRequestRejectedEvent;
import com.stop.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParticipationRequestRejectedEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "match.participation.request.rejected", groupId = "notification-service-group")
    public void consume(String payload) {
        ParticipationRequestRejectedEvent event;
        try {
            event = objectMapper.readValue(payload, ParticipationRequestRejectedEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize ParticipationRequestRejectedEvent: {}", payload, e);
            return;
        }

        log.info("Received match.participation.request.rejected. matchId={} userId={}", event.matchId(), event.userId());

        notificationService.save(Notification.builder()
                .userId(event.userId())
                .type(NotificationType.PARTICIPATION_REQUEST_REJECTED)
                .title("Katılım İsteği Reddedildi")
                .message("\"" + event.matchTitle() + "\" maçına katılım isteğin reddedildi.")
                .referenceId(event.matchId())
                .build());
    }
}
