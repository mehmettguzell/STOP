package com.stop.match_service.invitation.kafka.producer;

import com.stop.match_service.invitation.kafka.event.InvitationSentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationSentProducer {

    private static final String TOPIC = "match.invitation.sent";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(InvitationSentEvent event) {
        kafkaTemplate.send(TOPIC, event.matchId().toString(), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Failed to publish InvitationSentEvent. matchId={} receiverId={}", event.matchId(), event.receiverId(), error);
                    } else {
                        log.info("Published InvitationSentEvent. matchId={} receiverId={}", event.matchId(), event.receiverId());
                    }
                });
    }
}
