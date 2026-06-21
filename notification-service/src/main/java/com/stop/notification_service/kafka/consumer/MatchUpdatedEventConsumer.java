package com.stop.notification_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stop.notification_service.entity.Notification;
import com.stop.notification_service.entity.NotificationType;
import com.stop.notification_service.kafka.event.MatchUpdatedEvent;
import com.stop.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchUpdatedEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "match.match.updated", groupId = "notification-service-group")
    public void consume(String payload) {
        MatchUpdatedEvent event;
        try {
            event = objectMapper.readValue(payload, MatchUpdatedEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize MatchUpdatedEvent: {}", payload, e);
            return;
        }

        log.info("Received match.match.updated. matchId={}", event.matchId());

        String message = buildMessage(event);

        List<Notification> notifications = event.participantIds().stream()
                .filter(userId -> !userId.equals(event.organizerId()))
                .map(userId -> Notification.builder()
                        .userId(userId)
                        .type(NotificationType.MATCH_UPDATED)
                        .title("Maç Güncellendi")
                        .message(message)
                        .referenceId(event.matchId())
                        .build())
                .toList();

        notificationService.saveAll(notifications);
    }

    private String buildMessage(MatchUpdatedEvent event) {
        List<String> changes = event.changes();
        if (changes == null || changes.isEmpty()) {
            return "Katıldığın maçın detayları değişti.";
        }
        String joined = String.join(", ", changes);
        String full = "Maç güncellendi: " + joined;
        return full.length() > 80 ? full.substring(0, 79) + "…" : full;
    }
}
