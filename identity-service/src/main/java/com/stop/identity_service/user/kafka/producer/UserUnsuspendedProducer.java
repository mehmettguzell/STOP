package com.stop.identity_service.user.kafka.producer;

import com.stop.identity_service.user.kafka.event.UserUnsuspendedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserUnsuspendedProducer {
    public static final String USER_UNSUSPENDED_TOPIC = "identity.user.unsuspended";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(UserUnsuspendedEvent event) {
        kafkaTemplate.send(USER_UNSUSPENDED_TOPIC, event.userId().toString(), event)
            .whenComplete((result, error) -> {
                if (error != null) {
                    log.error("Failed to publish identity.user.unsuspended event userId={}", event.userId(), error);
                } else {
                    log.info("Published identity.user.unsuspended event userId={}", event.userId());
                }
            });
    }
}
