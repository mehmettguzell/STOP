package com.stop.notification_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stop.notification_service.entity.Notification;
import com.stop.notification_service.entity.NotificationType;
import com.stop.notification_service.kafka.event.InvitationSentEvent;
import com.stop.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvitationSentEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "match.invitation.sent", groupId = "notification-service-group")
    public void consume(String payload) {
        InvitationSentEvent event;
        try {
            event = objectMapper.readValue(payload, InvitationSentEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize InvitationSentEvent: {}", payload, e);
            return;
        }

        log.info("Received match.invitation.sent. matchId={} receiverId={}", event.matchId(), event.receiverId());

        notificationService.save(Notification.builder()
                .userId(event.receiverId())
                .type(NotificationType.INVITATION_RECEIVED)
                .title("Maç Daveti")
                .message("Bir maça davet edildin.")
                .referenceId(event.matchId())
                .build());
    }
}
