package com.stop.match_service.match.kafka.producer;

import com.stop.match_service.match.kafka.event.MatchCancelledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchCancelledProducer {
    public final static String MATCH_CANCELLED_TOPIC = "match.match.cancelled";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(MatchCancelledEvent event){
        kafkaTemplate.send(MATCH_CANCELLED_TOPIC, event.matchId().toString(), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Failed to publish MatchCancelledEvent for matchId: {}. Reason: {}",
                                event.matchId(), error.getMessage());
                    } else {
                        log.info("Successfully published MatchCancelledEvent to topic: {} for matchId: {}",
                                MATCH_CANCELLED_TOPIC, event.matchId());
                    }
                });
    }
}
