package com.stop.notification_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stop.notification_service.entity.Notification;
import com.stop.notification_service.entity.NotificationType;
import com.stop.notification_service.kafka.event.WaitlistPromotedEvent;
import com.stop.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaitlistPromotedEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "match.waitlist.promoted", groupId = "notification-service-group")
    public void consume(String payload) {
        WaitlistPromotedEvent event;
        try {
            event = objectMapper.readValue(payload, WaitlistPromotedEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize WaitlistPromotedEvent: {}", payload, e);
            return;
        }

        log.info("Received match.waitlist.promoted. matchId={} userId={}", event.matchId(), event.userId());

        // Bekleme listesinden terfi eden kullanıcıya bildirim gider (organizatöre değil).
        notificationService.save(Notification.builder()
                .userId(event.userId())
                .type(NotificationType.WAITLIST_PROMOTED)
                .title("Maça Alındın!")
                .message("Bekleme listesinden maça alındın!")
                .referenceId(event.matchId())
                .build());
    }
}
