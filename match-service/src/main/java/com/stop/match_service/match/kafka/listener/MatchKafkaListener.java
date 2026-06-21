package com.stop.match_service.match.kafka.listener;

import com.stop.match_service.invitation.kafka.event.InvitationSentEvent;
import com.stop.match_service.invitation.kafka.producer.InvitationSentProducer;
import com.stop.match_service.match.kafka.event.MatchCancelledEvent;
import com.stop.match_service.match.kafka.event.MatchCompletedEvent;
import com.stop.match_service.match.kafka.event.MatchFinalizedEvent;
import com.stop.match_service.match.kafka.event.MatchStartedEvent;
import com.stop.match_service.match.kafka.event.MatchUpdatedEvent;
import com.stop.match_service.match.kafka.producer.MatchCancelledProducer;
import com.stop.match_service.match.kafka.producer.MatchCompletedProducer;
import com.stop.match_service.match.kafka.producer.MatchFinalizedProducer;
import com.stop.match_service.match.kafka.producer.MatchStartedProducer;
import com.stop.match_service.match.kafka.producer.MatchUpdatedProducer;
import com.stop.match_service.matchParticipation.kafka.event.ParticipantJoinedEvent;
import com.stop.match_service.matchParticipation.kafka.event.ParticipantLeftEvent;
import com.stop.match_service.matchParticipation.kafka.event.ParticipantRemovedEvent;
import com.stop.match_service.matchParticipation.kafka.event.ParticipationRequestApprovedEvent;
import com.stop.match_service.matchParticipation.kafka.event.ParticipationRequestRejectedEvent;
import com.stop.match_service.matchParticipation.kafka.event.ParticipationRequestSentEvent;
import com.stop.match_service.matchParticipation.kafka.producer.ParticipantJoinedProducer;
import com.stop.match_service.matchParticipation.kafka.producer.ParticipantLeftProducer;
import com.stop.match_service.matchParticipation.kafka.producer.ParticipantRemovedProducer;
import com.stop.match_service.matchParticipation.kafka.producer.ParticipationRequestApprovedProducer;
import com.stop.match_service.matchParticipation.kafka.producer.ParticipationRequestRejectedProducer;
import com.stop.match_service.matchParticipation.kafka.producer.ParticipationRequestSentProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MatchKafkaListener {

    private final MatchStartedProducer matchStartedProducer;
    private final MatchCompletedProducer matchCompletedProducer;
    private final MatchCancelledProducer matchCancelledProducer;
    private final MatchUpdatedProducer matchUpdatedProducer;
    private final MatchFinalizedProducer matchFinalizedProducer;
    private final InvitationSentProducer invitationSentProducer;
    private final ParticipantJoinedProducer participantJoinedProducer;
    private final ParticipantLeftProducer participantLeftProducer;
    private final ParticipantRemovedProducer participantRemovedProducer;
    private final ParticipationRequestSentProducer participationRequestSentProducer;
    private final ParticipationRequestApprovedProducer participationRequestApprovedProducer;
    private final ParticipationRequestRejectedProducer participationRequestRejectedProducer;

    @Async("kafkaEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMatchStarted(MatchStartedEvent event) { matchStartedProducer.publish(event); }

    @Async("kafkaEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMatchCompleted(MatchCompletedEvent event) { matchCompletedProducer.publish(event); }

    @Async("kafkaEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMatchCancelled(MatchCancelledEvent event) { matchCancelledProducer.publish(event); }

    @Async("kafkaEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMatchUpdated(MatchUpdatedEvent event) { matchUpdatedProducer.publish(event); }

    @Async("kafkaEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMatchFinalized(MatchFinalizedEvent event) { matchFinalizedProducer.publish(event); }

    @Async("kafkaEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvitationSent(InvitationSentEvent event) { invitationSentProducer.publish(event); }

    @Async("kafkaEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onParticipantJoined(ParticipantJoinedEvent event) { participantJoinedProducer.publish(event); }

    @Async("kafkaEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onParticipantLeft(ParticipantLeftEvent event) { participantLeftProducer.publish(event); }

    @Async("kafkaEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onParticipantRemoved(ParticipantRemovedEvent event) { participantRemovedProducer.publish(event); }

    @Async("kafkaEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onParticipationRequestSent(ParticipationRequestSentEvent event) { participationRequestSentProducer.publish(event); }

    @Async("kafkaEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onParticipationRequestApproved(ParticipationRequestApprovedEvent event) { participationRequestApprovedProducer.publish(event); }

    @Async("kafkaEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onParticipationRequestRejected(ParticipationRequestRejectedEvent event) { participationRequestRejectedProducer.publish(event); }
}
