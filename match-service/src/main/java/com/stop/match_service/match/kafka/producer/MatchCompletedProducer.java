package com.stop.match_service.match.kafka.producer;

import com.stop.match_service.match.kafka.event.MatchCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchCompletedProducer {
    private final static String MATCH_COMPLETED_TOPIC = "match.match.completed";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(MatchCompletedEvent event)
    {
        kafkaTemplate.send(MATCH_COMPLETED_TOPIC, event.matchId().toString() ,event)
                .whenComplete((result,error)->{
                    if (error != null) {
                        log.error("Failed to publish MatchCompletedEvent for matchId: {}. Error: {}",
                                event.matchId(), error.getMessage());
                    } else {
                        log.info("Successfully published MatchCompletedEvent to topic: {} for matchId: {}",
                                MATCH_COMPLETED_TOPIC, event.matchId());
                    }
                });
    }
}
