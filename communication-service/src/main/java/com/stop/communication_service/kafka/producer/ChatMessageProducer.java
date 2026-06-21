package com.stop.communication_service.kafka.producer;

import com.stop.communication_service.kafka.event.ChatMessageSentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageProducer {

    private static final String TOPIC = "communication.chat.message.sent";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(ChatMessageSentEvent event) {
        kafkaTemplate.send(TOPIC, event.chatId().toString(), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Failed to publish ChatMessageSentEvent. chatId={}", event.chatId(), error);
                    } else {
                        log.debug("Published ChatMessageSentEvent. chatId={} senderId={}", event.chatId(), event.senderId());
                    }
                });
    }
}
