package com.stop.notification_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stop.notification_service.entity.Notification;
import com.stop.notification_service.entity.NotificationType;
import com.stop.notification_service.kafka.event.ParticipationRequestSentEvent;
import com.stop.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParticipationRequestSentEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "match.participation.request.sent", groupId = "notification-service-group")
    public void consume(String payload) {
        ParticipationRequestSentEvent event;
        try {
            event = objectMapper.readValue(payload, ParticipationRequestSentEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize ParticipationRequestSentEvent: {}", payload, e);
            return;
        }

        log.info("Received match.participation.request.sent. matchId={} userId={}", event.matchId(), event.userId());

        notificationService.save(Notification.builder()
                .userId(event.organizerId())
                .type(NotificationType.PARTICIPATION_REQUEST_RECEIVED)
                .title("Yeni Katılım İsteği")
                .message("\"" + event.matchTitle() + "\" maçına katılım isteği geldi.")
                .referenceId(event.matchId())
                .build());
    }
}
