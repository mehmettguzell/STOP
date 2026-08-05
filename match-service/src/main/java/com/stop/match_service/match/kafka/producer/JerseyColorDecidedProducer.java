package com.stop.match_service.match.kafka.producer;

import com.stop.match_service.match.kafka.event.JerseyColorDecidedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JerseyColorDecidedProducer {

    private static final String TOPIC = "match.match.jersey.decided";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(JerseyColorDecidedEvent event) {
        kafkaTemplate.send(TOPIC, event.matchId().toString(), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Failed to publish JerseyColorDecidedEvent. matchId={}", event.matchId(), error);
                    } else {
                        log.info("Published JerseyColorDecidedEvent. matchId={}", event.matchId());
                    }
                });
    }
}
