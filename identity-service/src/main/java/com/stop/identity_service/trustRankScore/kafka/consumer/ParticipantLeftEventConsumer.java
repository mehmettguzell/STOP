package com.stop.identity_service.trustRankScore.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stop.identity_service.trustRankScore.ScoreDeltaConstants;
import com.stop.identity_service.trustRankScore.entity.RankScoreEventType;
import com.stop.identity_service.trustRankScore.entity.TrustScoreEventType;
import com.stop.identity_service.trustRankScore.kafka.event.ParticipantLeftEvent;
import com.stop.identity_service.trustRankScore.service.RankScoreService;
import com.stop.identity_service.trustRankScore.service.TrustScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParticipantLeftEventConsumer {

    private static final String TOPIC  = "match.participant.left";
    private static final String SOURCE = "MATCH";

    private final TrustScoreService trustScoreService;
    private final RankScoreService  rankScoreService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = TOPIC, groupId = "identity-service-group")
    public void consume(String payload) {
        ParticipantLeftEvent event;
        try {
            event = objectMapper.readValue(payload, ParticipantLeftEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize ParticipantLeftEvent: {}", payload, e);
            return;
        }

        log.info("Received match.participant.left event. matchId={} userId={}",
                event.matchId(), event.userId());

        trustScoreService.applyDelta(
                event.userId(),
                ScoreDeltaConstants.TRUST_MATCH_LEFT_EARLY,
                TrustScoreEventType.MATCH_LEFT_EARLY,
                event.matchId(),
                SOURCE
        );

        rankScoreService.applyDelta(
                event.userId(),
                ScoreDeltaConstants.RANK_MATCH_LEFT_EARLY,
                RankScoreEventType.MATCH_LEFT_EARLY,
                event.matchId(),
                SOURCE
        );

        log.info("Scores penalized for early leave. userId={} matchId={}",
                event.userId(), event.matchId());
    }
}
