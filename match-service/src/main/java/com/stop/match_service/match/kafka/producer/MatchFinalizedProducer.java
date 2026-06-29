package com.stop.match_service.match.kafka.producer;

import com.stop.match_service.match.kafka.event.MatchFinalizedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchFinalizedProducer {

    private static final String TOPIC = "match.match.finalized";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(MatchFinalizedEvent event) {
        kafkaTemplate.send(TOPIC, event.matchId().toString(), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Failed to publish MatchFinalizedEvent. matchId={}", event.matchId(), error);
                    } else {
                        log.info("Published MatchFinalizedEvent. matchId={}", event.matchId());
                    }
                });
    }
}
