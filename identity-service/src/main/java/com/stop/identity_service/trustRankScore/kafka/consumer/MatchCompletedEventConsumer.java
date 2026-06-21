package com.stop.identity_service.trustRankScore.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stop.identity_service.trustRankScore.ScoreDeltaConstants;
import com.stop.identity_service.trustRankScore.entity.TrustScoreEventType;
import com.stop.identity_service.trustRankScore.kafka.event.MatchCompletedEvent;
import com.stop.identity_service.trustRankScore.service.TrustScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchCompletedEventConsumer {

    private static final String TOPIC  = "match.match.completed";
    private static final String SOURCE = "MATCH";

    private final TrustScoreService trustScoreService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = TOPIC, groupId = "identity-service-group")
    public void consume(String payload) {
        try {
            MatchCompletedEvent event = objectMapper.readValue(payload, MatchCompletedEvent.class);
            log.info("Received match.match.completed. matchId={} participants={}",
                    event.matchId(), event.participantIds().size());

            for (var userId : event.participantIds()) {
                trustScoreService.applyDelta(
                        userId,
                        ScoreDeltaConstants.TRUST_MATCH_COMPLETED,
                        TrustScoreEventType.MATCH_COMPLETED,
                        event.matchId(),
                        SOURCE
                );
            }

            log.info("Trust scores updated for match.match.completed. matchId={}", event.matchId());
        } catch (Exception e) {
            log.error("Failed to process match.match.completed payload: {}", payload, e);
        }
    }
}
