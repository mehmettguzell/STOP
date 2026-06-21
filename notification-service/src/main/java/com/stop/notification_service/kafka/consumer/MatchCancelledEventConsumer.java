package com.stop.notification_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stop.notification_service.entity.Notification;
import com.stop.notification_service.entity.NotificationType;
import com.stop.notification_service.kafka.event.MatchCancelledEvent;
import com.stop.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchCancelledEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "match.match.cancelled", groupId = "notification-service-group")
    public void consume(String payload) {
        MatchCancelledEvent event;
        try {
            event = objectMapper.readValue(payload, MatchCancelledEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize MatchCancelledEvent: {}", payload, e);
            return;
        }

        log.info("Received match.match.cancelled. matchId={}", event.matchId());

        List<Notification> notifications = event.participantIds().stream()
                .filter(userId -> !userId.equals(event.organizerId()))
                .map(userId -> Notification.builder()
                        .userId(userId)
                        .type(NotificationType.MATCH_CANCELLED)
                        .title("Maç İptal Edildi")
                        .message("Katıldığın maç organizatör tarafından iptal edildi.")
                        .referenceId(event.matchId())
                        .build())
                .toList();

        notificationService.saveAll(notifications);
    }
}
