package com.stop.match_service.match.kafka.producer;

import com.stop.match_service.match.kafka.event.MatchStartedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchStartedProducer {
    private final static String MATCH_STARTED_TOPIC = "match.match.started";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(MatchStartedEvent matchStartedEvent) {
        kafkaTemplate.send(MATCH_STARTED_TOPIC, matchStartedEvent.matchId().toString() ,matchStartedEvent)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Failed to publish MatchStartedEvent for matchId: {}. Error: {}",
                                matchStartedEvent.matchId(), error.getMessage());
                    } else {
                        log.info("Successfully published MatchStartedEvent to topic: {} for matchId: {}",
                                MATCH_STARTED_TOPIC, matchStartedEvent.matchId());
                    }
                });
    }
}
