package com.stop.match_service.match.service;

import com.stop.match_service.common.error.MatchRatingErrorCode;
import com.stop.match_service.common.exception.BusinessException;
import com.stop.match_service.match.dto.request.MatchRatingReq;
import com.stop.match_service.match.entity.Match;
import com.stop.match_service.match.entity.MatchRating;
import com.stop.match_service.match.entity.Status;
import com.stop.match_service.match.kafka.event.MatchFinalizedEvent;
import com.stop.match_service.match.repository.MatchRatingRepository;
import org.springframework.context.ApplicationEventPublisher;
import com.stop.match_service.matchParticipation.entity.MatchParticipant;
import com.stop.match_service.matchParticipation.entity.ParticipantStatus;
import com.stop.match_service.matchParticipation.entity.TeamType;
import com.stop.match_service.matchParticipation.repository.MatchParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchRatingService {

    private final MatchService matchService;
    private final MatchRatingRepository repository;
    private final MatchParticipantRepository participantRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void submitRating(UUID raterId, UUID matchId, MatchRatingReq matchRatingReq) {
        Match match = matchService.findMatchById(matchId);
        checkRaterAndRated(raterId, matchRatingReq.ratedId(), match);
        isRatingsClosed(match);
        repository.save(mapToEntity(raterId, matchRatingReq, match));
        match.setRatingCount(match.getRatingCount() + 1);
        if (isEveryoneRated(match)) {
            finalizeRatings(match);
        }
    }

    @Transactional
    public void finalizeRatings(Match match) {
        matchService.setStatusToRatingClosed(match);

        Map<UUID, TeamType> teams = participantRepository
                .findAllByMatchIdAndStatus(match.getId(), ParticipantStatus.JOINED)
                .stream()
                .collect(Collectors.toMap(MatchParticipant::getUserId, MatchParticipant::getTeam));

        Map<UUID, BigDecimal> avgScores = repository.findAvgScoresByMatchId(match.getId())
                .stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> BigDecimal.valueOf((Double) row[1]).setScale(2, RoundingMode.HALF_UP)
                ));

        eventPublisher.publishEvent(new MatchFinalizedEvent(
                match.getId(),
                match.getOrganizerId(),
                match.getHomeScore(),
                match.getAwayScore(),
                teams,
                avgScores,
                Instant.now()
        ));

        log.info("Ratings finalized. matchId={} participants={}", match.getId(), avgScores.size());
    }

    /*
     * =========================
     * PRIVATE — VALIDATIONS
     * =========================
     */

    private void isRatingsClosed(Match match) {
        if (match.getStatus() == Status.RATINGS_CLOSED) {
            throw new BusinessException(MatchRatingErrorCode.RATING_CLOSED);
        }
        if (match.getStatus() != Status.COMPLETED) {
            throw new BusinessException(MatchRatingErrorCode.MATCH_NOT_COMPLETED);
        }
    }

    private void checkRaterAndRated(UUID raterId, UUID ratedId, Match match) {
        if (raterId.equals(ratedId)) {
            throw new BusinessException(MatchRatingErrorCode.SELF_RATING_NOT_ALLOWED);
        }
        matchService.checkIsParticipant(match, raterId);
        matchService.checkIsParticipant(match, ratedId);
    }

    private boolean isEveryoneRated(Match match) {
        int n = match.getParticipantCount();
        long expected = (long) n * (n - 1);
        return match.getRatingCount() >= expected;
    }

    /*
     * =========================
     * PRIVATE — MAPPERS
     * =========================
     */

    private MatchRating mapToEntity(UUID raterId, MatchRatingReq matchRatingReq, Match match) {
        return MatchRating.builder()
                .match(match)
                .raterId(raterId)
                .ratedId(matchRatingReq.ratedId())
                .skill(matchRatingReq.skill())
                .teamwork(matchRatingReq.teamwork())
                .sportsmanship(matchRatingReq.sportsmanship())
                .build();
    }
}
