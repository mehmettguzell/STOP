package com.stop.identity_service.friendship.kafka.listener;

import com.stop.identity_service.friendship.kafka.event.FriendRequestAcceptedEvent;
import com.stop.identity_service.friendship.kafka.event.FriendRequestSentEvent;
import com.stop.identity_service.friendship.kafka.producer.FriendRequestAcceptedProducer;
import com.stop.identity_service.friendship.kafka.producer.FriendRequestSentProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class FriendshipKafkaEventListener {

    private final FriendRequestSentProducer sentProducer;
    private final FriendRequestAcceptedProducer acceptedProducer;

    @Async("kafkaEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFriendRequestSent(FriendRequestSentEvent event) {
        sentProducer.publish(event);
    }

    @Async("kafkaEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFriendRequestAccepted(FriendRequestAcceptedEvent event) {
        acceptedProducer.publish(event);
    }
}
