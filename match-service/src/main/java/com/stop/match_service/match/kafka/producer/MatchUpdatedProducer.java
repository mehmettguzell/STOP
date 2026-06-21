package com.stop.match_service.match.kafka.producer;

import com.stop.match_service.match.kafka.event.MatchUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchUpdatedProducer {

    private static final String TOPIC = "match.match.updated";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(MatchUpdatedEvent event) {
        kafkaTemplate.send(TOPIC, event.matchId().toString(), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Failed to publish MatchUpdatedEvent. matchId={}", event.matchId(), error);
                    } else {
                        log.info("Published MatchUpdatedEvent. matchId={}", event.matchId());
                    }
                });
    }
}
