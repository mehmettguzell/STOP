package com.stop.notification_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stop.notification_service.entity.Notification;
import com.stop.notification_service.entity.NotificationType;
import com.stop.notification_service.kafka.event.MatchCompletedEvent;
import com.stop.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchCompletedEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "match.match.completed", groupId = "notification-service-group")
    public void consume(String payload) {
        MatchCompletedEvent event;
        try {
            event = objectMapper.readValue(payload, MatchCompletedEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize MatchCompletedEvent: {}", payload, e);
            return;
        }

        log.info("Received match.match.completed. matchId={}", event.matchId());

        List<Notification> notifications = event.participantIds().stream()
                .map(userId -> Notification.builder()
                        .userId(userId)
                        .type(NotificationType.MATCH_COMPLETED)
                        .title("Maç Tamamlandı")
                        .message("Maç sona erdi! Takım arkadaşlarını değerlendirmeyi unutma.")
                        .referenceId(event.matchId())
                        .build())
                .toList();

        notificationService.saveAll(notifications);
    }
}
