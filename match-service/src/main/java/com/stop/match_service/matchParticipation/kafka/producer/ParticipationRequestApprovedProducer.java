package com.stop.match_service.matchParticipation.kafka.producer;

import com.stop.match_service.matchParticipation.kafka.event.ParticipationRequestApprovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationRequestApprovedProducer {

    private static final String TOPIC = "match.participation.request.approved";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(ParticipationRequestApprovedEvent event) {
        kafkaTemplate.send(TOPIC, event.matchId().toString(), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Failed to publish ParticipationRequestApprovedEvent. matchId={} userId={}", event.matchId(), event.userId(), error);
                    } else {
                        log.info("Published ParticipationRequestApprovedEvent. matchId={} userId={}", event.matchId(), event.userId());
                    }
                });
    }
}
