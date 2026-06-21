package com.stop.identity_service.friendship.kafka.producer;

import com.stop.identity_service.friendship.kafka.event.FriendRequestSentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendRequestSentProducer {
    public static final String TOPIC = "identity.friend.request.sent";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(FriendRequestSentEvent event) {
        kafkaTemplate.send(TOPIC, event.receiverId().toString(), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Failed to publish friend request sent event friendshipId={}", event.friendshipId(), error);
                    } else {
                        log.info("Published friend request sent event friendshipId={}", event.friendshipId());
                    }
                });
    }
}
