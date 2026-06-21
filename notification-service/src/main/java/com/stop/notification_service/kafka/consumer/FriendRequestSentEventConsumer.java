package com.stop.notification_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stop.notification_service.entity.Notification;
import com.stop.notification_service.entity.NotificationType;
import com.stop.notification_service.kafka.event.FriendRequestSentEvent;
import com.stop.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FriendRequestSentEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "identity.friend.request.sent", groupId = "notification-service-group")
    public void consume(String payload) {
        FriendRequestSentEvent event;
        try {
            event = objectMapper.readValue(payload, FriendRequestSentEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize FriendRequestSentEvent: {}", payload, e);
            return;
        }

        log.info("Received identity.friend.request.sent. friendshipId={} receiverId={}",
                event.friendshipId(), event.receiverId());

        notificationService.save(Notification.builder()
                .userId(event.receiverId())
                .type(NotificationType.FRIEND_REQUEST_RECEIVED)
                .title("Arkadaşlık İsteği")
                .message("Sana arkadaşlık isteği gönderildi.")
                .referenceId(event.requesterId())
                .build());
    }
}
