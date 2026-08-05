package com.stop.notification_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stop.notification_service.entity.Notification;
import com.stop.notification_service.entity.NotificationType;
import com.stop.notification_service.kafka.event.JerseyColorDecidedEvent;
import com.stop.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JerseyColorDecidedEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "match.match.jersey.decided", groupId = "notification-service-group")
    public void consume(String payload) {
        JerseyColorDecidedEvent event;
        try {
            event = objectMapper.readValue(payload, JerseyColorDecidedEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize JerseyColorDecidedEvent: {}", payload, e);
            return;
        }

        log.info("Received match.match.jersey.decided. matchId={} white={} black={}",
                event.matchId(), event.whiteTeamUserIds().size(), event.blackTeamUserIds().size());

        List<Notification> notifications = new ArrayList<>();
        notifications.addAll(buildNotifications(event.matchId(), event.whiteTeamUserIds(), "Bu maçta beyaz forma giyeceksin."));
        notifications.addAll(buildNotifications(event.matchId(), event.blackTeamUserIds(), "Bu maçta siyah forma giyeceksin."));

        notificationService.saveAll(notifications);
    }

    private List<Notification> buildNotifications(UUID matchId, List<UUID> userIds, String message) {
        return userIds.stream()
                .map(userId -> Notification.builder()
                        .userId(userId)
                        .type(NotificationType.JERSEY_COLOR_DECIDED)
                        .title("Forma Renginiz Belirlendi")
                        .message(message)
                        .referenceId(matchId)
                        .build())
                .toList();
    }
}
