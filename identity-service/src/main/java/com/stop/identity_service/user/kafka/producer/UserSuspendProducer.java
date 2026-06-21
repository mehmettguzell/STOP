package com.stop.identity_service.user.kafka.producer;

import com.stop.identity_service.user.kafka.event.UserSuspendedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSuspendProducer {
    public static final String USER_SUSPEND_TOPIC = "identity.user.suspended";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(UserSuspendedEvent event) {
        kafkaTemplate.send(USER_SUSPEND_TOPIC, event.userId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish identity.user.suspended event userId={}", event.userId(), ex);
                } else {
                    log.info("Published identity.user.suspended event userId={}", event.userId());
                }
            });
    }
}
