package com.stop.identity_service.user.kafka.producer;

import com.stop.identity_service.user.kafka.event.UserUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserUpdatedProducer {
    public static final String USER_UPDATED_TOPIC = "identity.user.updated";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(UserUpdatedEvent event) {
        kafkaTemplate.send(USER_UPDATED_TOPIC, event.userId().toString(), event)
            .whenComplete((result, error) -> {
                if (error != null) {
                    log.error("Failed to publish identity.user.updated event userId={}", event.userId(), error);
                } else {
                    log.info("Published identity.user.updated event userId={}", event.userId());
                }
            });
    }
}
