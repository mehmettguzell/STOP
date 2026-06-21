package com.stop.identity_service.trustRankScore.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stop.identity_service.trustRankScore.ScoreDeltaConstants;
import com.stop.identity_service.trustRankScore.entity.RankScoreEventType;
import com.stop.identity_service.trustRankScore.kafka.event.MatchFinalizedEvent;
import com.stop.identity_service.trustRankScore.service.RankScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchFinalizedEventConsumer {

    private static final String TOPIC  = "match.match.finalized";
    private static final String SOURCE = "MATCH";

    private static final BigDecimal PEER_RATING_MULTIPLIER = new BigDecimal("0.2");

    private final RankScoreService rankScoreService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = TOPIC, groupId = "identity-service-group")
    public void consume(String payload) {
        MatchFinalizedEvent event;
        try {
            event = objectMapper.readValue(payload, MatchFinalizedEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize MatchFinalizedEvent: {}", payload, e);
            return;
        }

        log.info("Received match.match.finalized. matchId={} homeScore={} awayScore={}",
                event.matchId(), event.homeScore(), event.awayScore());

        String winnerTeam = resolveWinner(event.homeScore(), event.awayScore());

        for (var entry : event.teams().entrySet()) {
            UUID userId   = entry.getKey();
            String team   = entry.getValue();

            // 1. Kazanma / Kaybetme
            if (winnerTeam != null) {
                boolean won = team.equals(winnerTeam);
                rankScoreService.applyDelta(
                        userId,
                        won ? ScoreDeltaConstants.RANK_MATCH_WIN : ScoreDeltaConstants.RANK_MATCH_LOSE,
                        won ? RankScoreEventType.MATCH_WIN : RankScoreEventType.MATCH_LOSE,
                        event.matchId(),
                        SOURCE
                );
            }

            // 2. Peer rating — (avg - 3.0) * 0.2
            BigDecimal avg = event.avgScores().get(userId);
            if (avg != null) {
                BigDecimal peerDelta = avg.subtract(new BigDecimal("3.0"))
                        .multiply(PEER_RATING_MULTIPLIER)
                        .setScale(2, RoundingMode.HALF_UP);

                rankScoreService.applyDelta(
                        userId,
                        peerDelta,
                        RankScoreEventType.PEER_RATING,
                        event.matchId(),
                        SOURCE
                );
            }
        }

        log.info("Rank scores updated for match.match.finalized. matchId={}", event.matchId());
    }

    /**
     * @return "HOME", "AWAY", veya null (beraberlik)
     */
    private String resolveWinner(Integer homeScore, Integer awayScore) {
        if (homeScore == null || awayScore == null) return null;
        if (homeScore > awayScore) return "HOME";
        if (awayScore > homeScore) return "AWAY";
        return null; // beraberlik — ne kazanma ne kaybetme puanı
    }
}
