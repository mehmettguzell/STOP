package com.stop.identity_service.user.kafka.listener;

import com.stop.identity_service.user.kafka.event.*;
import com.stop.identity_service.user.kafka.producer.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserKafkaEventListener {

    private final UserCreatedProducer userCreatedProducer;
    private final UserSuspendProducer userSuspendProducer;
    private final UserUnsuspendedProducer userUnsuspendedProducer;
    private final UserUpdatedProducer userUpdatedProducer;
    private final UserDeletedProducer userDeletedProducer;

    @Async("kafkaEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserCreated(UserCreatedEvent event) {
        userCreatedProducer.publish(event);
    }

    @Async("kafkaEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserSuspended(UserSuspendedEvent event) {
        userSuspendProducer.publish(event);
    }

    @Async("kafkaEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserUnsuspended(UserUnsuspendedEvent event) {
        userUnsuspendedProducer.publish(event);
    }

    @Async("kafkaEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserUpdated(UserUpdatedEvent event) {
        userUpdatedProducer.publish(event);
    }

    @Async("kafkaEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserDeleted(UserDeletedEvent event) {
        userDeletedProducer.publish(event);
    }
}
