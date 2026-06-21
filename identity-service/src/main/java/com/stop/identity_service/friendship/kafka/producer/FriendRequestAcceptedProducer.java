package com.stop.identity_service.friendship.kafka.producer;

import com.stop.identity_service.friendship.kafka.event.FriendRequestAcceptedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendRequestAcceptedProducer {
    public static final String TOPIC = "identity.friend.request.accepted";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(FriendRequestAcceptedEvent event) {
        kafkaTemplate.send(TOPIC, event.requesterId().toString(), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Failed to publish friend request accepted event friendshipId={}", event.friendshipId(), error);
                    } else {
                        log.info("Published friend request accepted event friendshipId={}", event.friendshipId());
                    }
                });
    }
}
