package com.stop.identity_service.user.kafka.producer;

import com.stop.identity_service.user.kafka.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCreatedProducer {
    public static final String USER_CREATED_TOPIC = "identity.user.created";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(UserCreatedEvent event) {
        kafkaTemplate.send(USER_CREATED_TOPIC, event.userId().toString(), event)
            .whenComplete((result, error) -> {
                if (error != null) {
                    log.error("Failed to publish identity.user.created event userId={}", event.userId(), error);
                } else {
                    log.info("Published identity.user.created event userId={}", event.userId());
                }
            });
    }
}
