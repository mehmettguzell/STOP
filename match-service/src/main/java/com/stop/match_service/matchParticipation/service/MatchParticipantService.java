package com.stop.match_service.matchParticipation.service;

import com.stop.match_service.common.error.MatchErrorCode;
import com.stop.match_service.common.error.ParticipationErrorCode;
import com.stop.match_service.common.exception.BusinessException;
import com.stop.match_service.match.entity.Match;
import com.stop.match_service.match.entity.Status;
import com.stop.match_service.match.repository.MatchRepository;
import com.stop.match_service.matchParticipation.dto.request.ParticipantRequestReq;
import com.stop.match_service.matchParticipation.dto.request.RemoveParticipationReq;
import com.stop.match_service.matchParticipation.dto.request.TeamAssignmentReq;
import com.stop.match_service.matchParticipation.dto.response.MatchParticipantResponse;
import com.stop.match_service.matchParticipation.entity.MatchParticipant;
import com.stop.match_service.matchParticipation.entity.ParticipantStatus;
import com.stop.match_service.matchParticipation.entity.TeamType;
import com.stop.match_service.matchParticipation.kafka.event.ParticipantJoinedEvent;
import com.stop.match_service.matchParticipation.kafka.event.ParticipantLeftEvent;
import com.stop.match_service.matchParticipation.kafka.event.ParticipantRemovedEvent;
import com.stop.match_service.matchParticipation.repository.MatchParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchParticipantService {

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository repository;
    private final ApplicationEventPublisher eventPublisher;


    @Transactional
    @CacheEvict(value = {"match:participants", "match:participant-ids"}, key = "#match.id")
    public void addInitialParticipant(Match match, UUID organizerId) {
        MatchParticipant organizer = MatchParticipant.builder()
                .match(match)
                .userId(organizerId)
                .status(ParticipantStatus.JOINED)
                .build();
        repository.save(organizer);
        eventPublisher.publishEvent(new ParticipantJoinedEvent(match.getId(), organizerId, organizerId, Instant.now()));
        log.debug("Organizer added as initial participant. matchId={} userId={}", match.getId(), organizerId);
    }

    @Transactional
    @CacheEvict(value = {"match:participants", "match:participant-ids"}, key = "#match.id")
    public void join(Match match, UUID userId) {
        repository.findByMatchIdAndUserId(match.getId(), userId)
                .ifPresentOrElse(
                        existing -> existing.setStatus(ParticipantStatus.JOINED),
                        () -> repository.save(MatchParticipant.builder()
                                .match(match)
                                .userId(userId)
                                .status(ParticipantStatus.JOINED)
                                .build())
                );

        match.setParticipantCount(match.getParticipantCount() + 1);
        if (match.getParticipantCount().equals(match.getCapacity())) {
            match.setStatus(Status.FULL);
        }

        eventPublisher.publishEvent(new ParticipantJoinedEvent(match.getId(), userId, match.getOrganizerId(), Instant.now()));
        log.info("User joined match. matchId={} userId={} participantCount={}",
                match.getId(), userId, match.getParticipantCount());
    }


    @Transactional
    @CacheEvict(value = {"match:participants", "match:participant-ids"}, key = "#matchId")
    public List<MatchParticipantResponse> assignTeams(UUID matchId, TeamAssignmentReq req, UUID organizerId) {
        log.info("Team assignment started. matchId={} organizerId={}", matchId, organizerId);

        Match match = findMatchById(matchId);
        checkIsOrganizer(match, organizerId);
        checkTeamAssignmentAllowed(match);

        List<MatchParticipant> joined = repository.findAllByMatchIdAndStatus(matchId, ParticipantStatus.JOINED);

        validateAssignmentCoversAllParticipants(joined, req.assignments());

        Map<UUID, TeamType> teamMap = req.assignments().stream()
                .collect(Collectors.toMap(
                        TeamAssignmentReq.PlayerTeamEntry::userId,
                        TeamAssignmentReq.PlayerTeamEntry::team
                ));

        joined.forEach(p -> p.setTeam(teamMap.get(p.getUserId())));

        log.info("Teams assigned. matchId={} home={} away={}",
                matchId,
                req.assignments().stream().filter(e -> e.team() == TeamType.HOME).count(),
                req.assignments().stream().filter(e -> e.team() == TeamType.AWAY).count()
        );

        return joined.stream().map(this::toResponse).toList();
    }


    @Transactional
    @CacheEvict(value = {"match:participants", "match:participant-ids"}, key = "#request.matchId")
    public MatchParticipantResponse leaveMatch(ParticipantRequestReq request, UUID userId) {
        log.info("User attempting to leave match. matchId={} userId={}", request.matchId(), userId);

        Match match = findMatchById(request.matchId());
        checkMatchEditable(match);
        checkNotTooLateToLeave(match);

        if (match.getOrganizerId().equals(userId)) {
            transferOrReplaceOrganizer(match, userId);
        }

        MatchParticipant participant = getJoinedParticipant(match.getId(), userId);
        participant.setStatus(ParticipantStatus.LEFT);
        decrementParticipantCount(match);

        eventPublisher.publishEvent(new ParticipantLeftEvent(match.getId(), userId, match.getOrganizerId(), Instant.now()));
        log.info("User left match. matchId={} userId={}", match.getId(), userId);
        return toResponse(participant);
    }

    @Transactional
    @CacheEvict(value = {"match:participants", "match:participant-ids"}, key = "#request.matchId")
    public MatchParticipantResponse removePlayer(RemoveParticipationReq request, UUID organizerId) {
        log.info("Organizer removing player. matchId={} targetUserId={} organizerId={}",
                request.matchId(), request.userId(), organizerId);

        Match match = findMatchById(request.matchId());
        checkIsOrganizer(match, organizerId);
        checkMatchEditable(match);

        if (match.getOrganizerId().equals(request.userId())) {
            transferOrReplaceOrganizer(match, request.userId());
        }

        MatchParticipant participant = getJoinedParticipant(match.getId(), request.userId());
        participant.setStatus(ParticipantStatus.REMOVED);
        decrementParticipantCount(match);

        eventPublisher.publishEvent(new ParticipantRemovedEvent(match.getId(), request.userId(), organizerId, Instant.now()));
        log.info("Player removed from match. matchId={} userId={}", match.getId(), request.userId());
        return toResponse(participant);
    }


    @Transactional(readOnly = true)
    @Cacheable(value = "match:participants", key = "#matchId")
    public List<MatchParticipantResponse> getMatchParticipants(UUID matchId) {
        findMatchById(matchId);
        return repository.findAllByMatchIdAndStatus(matchId, ParticipantStatus.JOINED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Cacheable(value = "match:participant-ids", key = "#matchId")
    public List<UUID> getMatchParticipantIds(UUID matchId) {
        return repository.findAllByMatchId(matchId);
    }

    public boolean isJoined(UUID matchId, UUID userId) {
        return repository.existsByMatchIdAndUserIdAndStatus(matchId, userId, ParticipantStatus.JOINED);
    }

    public List<MatchParticipant> findMyMatches(UUID userId, String title, int page, int size) {
        if (title != null && !title.isBlank()) {
            return repository.findMyMatchesSortedByTitle(userId, ParticipantStatus.JOINED, title.trim());
        }
        return repository.findMyMatchesSorted(userId, ParticipantStatus.JOINED);
    }

    /*
     * =====================
     * PRIVATE HELPERS
     * =====================
     */

    private void checkTeamAssignmentAllowed(Match match) {
        Status s = match.getStatus();
        if (s == Status.STARTED || s == Status.COMPLETED || s == Status.CANCELLED) {
            throw new BusinessException(ParticipationErrorCode.TEAM_ASSIGNMENT_NOT_ALLOWED);
        }
    }


    private void validateAssignmentCoversAllParticipants(
            List<MatchParticipant> joined,
            List<TeamAssignmentReq.PlayerTeamEntry> assignments) {

        Set<UUID> joinedIds = joined.stream()
                .map(MatchParticipant::getUserId)
                .collect(Collectors.toSet());

        Set<UUID> assignedIds = assignments.stream()
                .map(TeamAssignmentReq.PlayerTeamEntry::userId)
                .collect(Collectors.toSet());

        if (assignments.size() != assignedIds.size()) {
            throw new BusinessException(ParticipationErrorCode.INVALID_TEAM_ASSIGNMENT);
        }

        if (!joinedIds.equals(assignedIds)) {
            throw new BusinessException(ParticipationErrorCode.INVALID_TEAM_ASSIGNMENT);
        }
    }

    private void checkMatchEditable(Match match) {
        if (match.getStatus() == Status.STARTED
                || match.getStatus() == Status.COMPLETED
                || match.getStatus() == Status.CANCELLED) {
            throw new BusinessException(ParticipationErrorCode.INVALID_MATCH_STATUS_FOR_LEAVING);
        }
    }

    private void checkNotTooLateToLeave(Match match) {
        if (match.getStartTime().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BusinessException(ParticipationErrorCode.TOO_LATE_TO_LEAVE);
        }
    }

    private Match findMatchById(UUID matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(MatchErrorCode.MATCH_NOT_FOUND));
    }

    public void checkIsOrganizer(Match match, UUID userId) {
        if (!match.getOrganizerId().equals(userId)) {
            log.warn("Unauthorized action. matchId={} expectedOrganizer={} actualUser={}",
                    match.getId(), match.getOrganizerId(), userId);
            throw new BusinessException(MatchErrorCode.UNAUTHORIZED);
        }
    }

    private void transferOrReplaceOrganizer(Match match, UUID departingOrganizerId) {
        List<MatchParticipant> candidates = repository
                .findAllByMatchIdAndStatus(match.getId(), ParticipantStatus.JOINED)
                .stream()
                .filter(p -> !p.getUserId().equals(departingOrganizerId))
                .toList();

        if (candidates.isEmpty()) {
            match.setStatus(Status.CANCELLED);
            log.info("Match cancelled — last participant (organizer) left. matchId={}", match.getId());
        } else {
            UUID newOrganizerId = candidates.get(0).getUserId();
            match.setOrganizerId(newOrganizerId);
            log.info("Organizer transferred. matchId={} from={} to={}", match.getId(), departingOrganizerId, newOrganizerId);
        }
    }

    private void decrementParticipantCount(Match match) {
        match.setParticipantCount(match.getParticipantCount() - 1);
        if (match.getStatus() == Status.FULL) {
            match.setStatus(Status.OPEN);
        }
    }

    private MatchParticipant getJoinedParticipant(UUID matchId, UUID userId) {
        MatchParticipant participant = repository.findByMatchIdAndUserId(matchId, userId)
                .orElseThrow(() -> new BusinessException(ParticipationErrorCode.NOT_JOINED));
        if (participant.getStatus() != ParticipantStatus.JOINED) {
            throw new BusinessException(ParticipationErrorCode.NOT_JOINED);
        }
        return participant;
    }

    /*
     * =====================
     * DIZILIM POZİSYONLARI
     * =====================
     */

    @Transactional
    @CacheEvict(value = {"match:participants", "match:participant-ids"}, key = "#matchId")
    public List<MatchParticipantResponse> updateLineupPositions(UUID matchId, List<LineupEntry> entries, UUID organizerId) {
        log.info("Lineup positions update started. matchId={} organizerId={}", matchId, organizerId);

        Match match = findMatchById(matchId);
        checkIsOrganizer(match, organizerId);

        List<MatchParticipant> joined = repository.findAllByMatchIdAndStatus(matchId, ParticipantStatus.JOINED);

        Map<UUID, MatchParticipant> participantMap = joined.stream()
                .collect(Collectors.toMap(MatchParticipant::getUserId, p -> p));

        entries.forEach(entry -> {
            MatchParticipant p = participantMap.get(entry.userId());
            if (p != null) {
                p.setPositionX(entry.positionX());
                p.setPositionY(entry.positionY());
            }
        });

        log.info("Lineup positions updated. matchId={} count={}", matchId, entries.size());
        return joined.stream().map(this::toResponse).toList();
    }

    public record LineupEntry(UUID userId, Float positionX, Float positionY) {}

    private MatchParticipantResponse toResponse(MatchParticipant p) {
        return new MatchParticipantResponse(
                p.getId(),
                p.getMatch().getId(),
                p.getUserId(),
                p.getStatus(),
                p.getTeam(),
                p.getPositionX(),
                p.getPositionY()
        );
    }
}
