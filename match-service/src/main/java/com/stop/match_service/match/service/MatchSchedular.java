package com.stop.match_service.match.service;

import com.stop.match_service.match.entity.Match;
import com.stop.match_service.match.entity.Status;
import com.stop.match_service.match.kafka.event.MatchFinalizedEvent;
import com.stop.match_service.match.repository.MatchRatingRepository;
import com.stop.match_service.match.repository.MatchRepository;
import com.stop.match_service.matchParticipation.entity.MatchParticipant;
import com.stop.match_service.matchParticipation.entity.ParticipantStatus;
import com.stop.match_service.matchParticipation.entity.TeamType;
import com.stop.match_service.matchParticipation.repository.MatchParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchSchedular {

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository participantRepository;
    private final MatchRatingRepository matchRatingRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void closeMatchRatings() {
        List<Match> expired = matchRepository.findAllByStatusAndRatingDeadlineBefore(
                Status.COMPLETED, LocalDateTime.now());
        if (expired.isEmpty()) return;

        List<UUID> matchIds = expired.stream().map(Match::getId).toList();
        log.info("Closing ratings for {} expired matches", matchIds.size());

        matchRepository.updateStatusByIds(matchIds, Status.RATINGS_CLOSED);

        Map<UUID, Map<UUID, TeamType>> teams = participantRepository
                .findAllByMatchIdInAndStatus(matchIds, ParticipantStatus.JOINED)
                .stream()
                .collect(Collectors.groupingBy(
                        mp -> mp.getMatch().getId(),
                        Collectors.toMap(MatchParticipant::getUserId, MatchParticipant::getTeam)
                ));

        Map<UUID, Map<UUID, BigDecimal>> scores = matchRatingRepository
                .findAvgScoresByMatchIds(matchIds)
                .stream()
                .collect(Collectors.groupingBy(
                        row -> (UUID) row[0],
                        Collectors.toMap(
                                row -> (UUID) row[1],
                                row -> BigDecimal.valueOf((Double) row[2]).setScale(2, RoundingMode.HALF_UP)
                        )
                ));

        Instant now = Instant.now();
        expired.forEach(match -> eventPublisher.publishEvent(new MatchFinalizedEvent(
                match.getId(),
                match.getOrganizerId(),
                match.getHomeScore(),
                match.getAwayScore(),
                teams.getOrDefault(match.getId(), Map.of()),
                scores.getOrDefault(match.getId(), Map.of()),
                now
        )));
    }
}
