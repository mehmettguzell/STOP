package com.stop.identity_service.user.kafka.producer;

import com.stop.identity_service.user.kafka.event.UserDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeletedProducer {
    public static final String USER_DELETED_TOPIC = "identity.user.deleted";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(UserDeletedEvent event) {
        kafkaTemplate.send(USER_DELETED_TOPIC, event.userId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish identity.user.deleted event userId={}", event.userId(), ex);
                } else {
                    log.info("Published identity.user.deleted event userId={}", event.userId());
                }
            });
    }
}
