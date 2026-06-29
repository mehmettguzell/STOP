package com.stop.notification_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stop.notification_service.entity.Notification;
import com.stop.notification_service.entity.NotificationType;
import com.stop.notification_service.kafka.event.ChatMessageSentEvent;
import com.stop.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageSentEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "communication.chat.message.sent", groupId = "notification-service-group")
    public void consume(String payload) {
        ChatMessageSentEvent event;
        try {
            event = objectMapper.readValue(payload, ChatMessageSentEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize ChatMessageSentEvent: {}", payload, e);
            return;
        }

        log.info("Received communication.chat.message.sent. chatId={} senderId={} chatType={}",
                event.chatId(), event.senderId(), event.chatType());

        if (event.participantIds() == null || event.participantIds().isEmpty()) return;

        boolean isMatch = "MATCH".equals(event.chatType());
        String msgText = isMatch ? "Maç sohbetinde yeni bir mesaj var." : "Yeni bir mesaj aldın.";

        List<Notification> notifications = event.participantIds().stream()
                .filter(userId -> !userId.equals(event.senderId()))
                .map(userId -> Notification.builder()
                        .userId(userId)
                        .type(NotificationType.CHAT_MESSAGE_RECEIVED)
                        .title("Yeni Mesaj")
                        .message(msgText)
                        .referenceId(event.chatId())
                        .build())
                .toList();

        if (!notifications.isEmpty()) {
            notificationService.saveAll(notifications);
        }
    }
}
