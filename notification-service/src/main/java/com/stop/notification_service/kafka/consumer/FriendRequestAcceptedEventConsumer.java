package com.stop.notification_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stop.notification_service.entity.Notification;
import com.stop.notification_service.entity.NotificationType;
import com.stop.notification_service.kafka.event.FriendRequestAcceptedEvent;
import com.stop.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FriendRequestAcceptedEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "identity.friend.request.accepted", groupId = "notification-service-group")
    public void consume(String payload) {
        FriendRequestAcceptedEvent event;
        try {
            event = objectMapper.readValue(payload, FriendRequestAcceptedEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize FriendRequestAcceptedEvent: {}", payload, e);
            return;
        }

        log.info("Received identity.friend.request.accepted. friendshipId={} requesterId={}",
                event.friendshipId(), event.requesterId());

        notificationService.save(Notification.builder()
                .userId(event.requesterId())
                .type(NotificationType.FRIEND_REQUEST_ACCEPTED)
                .title("Arkadaşlık İsteği Kabul Edildi")
                .message("Arkadaşlık isteğin kabul edildi.")
                .referenceId(event.acceptorId())
                .build());
    }
}
