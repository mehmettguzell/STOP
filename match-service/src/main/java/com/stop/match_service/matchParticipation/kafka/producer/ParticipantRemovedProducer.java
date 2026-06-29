package com.stop.match_service.matchParticipation.kafka.producer;

import com.stop.match_service.matchParticipation.kafka.event.ParticipantRemovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipantRemovedProducer {

    private static final String TOPIC = "match.participant.removed";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(ParticipantRemovedEvent event) {
        kafkaTemplate.send(TOPIC, event.matchId().toString(), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Failed to publish ParticipantRemovedEvent. matchId={} userId={}", event.matchId(), event.userId(), error);
                    } else {
                        log.info("Published ParticipantRemovedEvent. matchId={} userId={}", event.matchId(), event.userId());
                    }
                });
    }
}
