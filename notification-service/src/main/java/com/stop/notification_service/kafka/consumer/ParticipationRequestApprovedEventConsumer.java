package com.stop.notification_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stop.notification_service.entity.Notification;
import com.stop.notification_service.entity.NotificationType;
import com.stop.notification_service.kafka.event.ParticipationRequestApprovedEvent;
import com.stop.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParticipationRequestApprovedEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "match.participation.request.approved", groupId = "notification-service-group")
    public void consume(String payload) {
        ParticipationRequestApprovedEvent event;
        try {
            event = objectMapper.readValue(payload, ParticipationRequestApprovedEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize ParticipationRequestApprovedEvent: {}", payload, e);
            return;
        }

        log.info("Received match.participation.request.approved. matchId={} userId={}", event.matchId(), event.userId());

        notificationService.save(Notification.builder()
                .userId(event.userId())
                .type(NotificationType.PARTICIPATION_REQUEST_APPROVED)
                .title("Katılım İsteği Onaylandı")
                .message("\"" + event.matchTitle() + "\" maçına katılım isteğin onaylandı.")
                .referenceId(event.matchId())
                .build());
    }
}
