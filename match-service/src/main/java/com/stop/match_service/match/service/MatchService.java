package com.stop.match_service.match.service;

import com.stop.match_service.common.error.MatchErrorCode;
import com.stop.match_service.common.error.ParticipationErrorCode;
import com.stop.match_service.common.exception.BusinessException;
import com.stop.match_service.match.dto.request.CompleteMatchReq;
import com.stop.match_service.match.dto.request.CreateMatchRequest;
import com.stop.match_service.match.dto.request.UpdateMatchRequest;
import com.stop.match_service.match.dto.response.MatchResponse;
import com.stop.match_service.match.entity.Match;
import com.stop.match_service.match.entity.Status;
import com.stop.match_service.match.entity.Visibility;
import com.stop.match_service.match.kafka.event.MatchCancelledEvent;
import com.stop.match_service.match.kafka.event.MatchCompletedEvent;
import com.stop.match_service.match.kafka.event.MatchStartedEvent;
import com.stop.match_service.match.kafka.event.MatchUpdatedEvent;
import com.stop.match_service.match.repository.MatchRepository;
import com.stop.match_service.match.repository.MatchSpecification;
import com.stop.match_service.config.jwt.SecurityUtils;
import com.stop.match_service.matchParticipation.service.MatchParticipantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final MatchParticipantService matchParticipantService;
    private final ApplicationEventPublisher eventPublisher;

    private static final int INITIAL_PARTICIPANT_COUNT = 1;

    /*
     * =========================
     * WRITE OPERATIONS
     * =========================
     */

    @Transactional
    public MatchResponse createMatch(CreateMatchRequest request, UUID organizerId) {
        log.info("Initiating match creation. OrganizerId: {}", organizerId);

        Match saved = matchRepository.save(mapToEntity(request, organizerId));
        matchParticipantService.addInitialParticipant(saved, organizerId);

        log.info("Match created. MatchId: {}, Status: {}, Capacity: {}",
                saved.getId(), saved.getStatus(), saved.getCapacity());

        return mapToResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "match:detail", key = "#matchId")
    public MatchResponse updateMatch(UpdateMatchRequest request, UUID matchId, UUID currentUserId) {
        log.info("Initiating match update. MatchId: {}, RequesterId: {}", matchId, currentUserId);

        Match match = findMatchById(matchId);
        checkOrganizer(currentUserId, match);
        checkEditable(match);

        List<String> changes = detectChanges(request, match);
        boolean updated = applyFieldUpdates(request, match);
        updated |= applyCapacityUpdate(match, request.capacity());
        if (request.capacity() != null && !request.capacity().equals(match.getCapacity())) {
            changes.add("Kapasite: " + request.capacity());
        }

        if (updated) {
            List<UUID> participantIds = matchParticipantService.getMatchParticipantIds(matchId);
            eventPublisher.publishEvent(new MatchUpdatedEvent(
                    match.getId(),
                    match.getOrganizerId(),
                    participantIds,
                    match.getTitle(),
                    match.getLocation(),
                    match.getStartTime(),
                    changes,
                    Instant.now()
            ));
            log.info("Match updated. MatchId: {}", matchId);
        } else {
            log.debug("No fields changed. MatchId: {}", matchId);
        }

        return mapToResponse(match);
    }

    @Transactional
    @CacheEvict(value = "match:detail", key = "#matchId")
    public void cancelMatch(UUID matchId, UUID currentUserId) {
        log.info("Initiating match cancellation. MatchId: {}, RequesterId: {}", matchId, currentUserId);

        Match match = findMatchById(matchId);
        checkOrganizer(currentUserId, match);
        checkCancellable(match);

        List<UUID> participantIds = matchParticipantService.getMatchParticipantIds(matchId);
        eventPublisher.publishEvent(new MatchCancelledEvent(match.getId(), match.getOrganizerId(), participantIds, Instant.now()));

        matchRepository.deleteById(matchId);
        log.info("Match deleted (cancelled). MatchId: {}", matchId);
    }

    @Transactional
    @CacheEvict(value = "match:detail", key = "#matchId")
    public MatchResponse transferOrganizer(UUID matchId, UUID newOrganizerId, UUID currentUserId) {
        log.info("Initiating organizer transfer. MatchId: {}, From: {}, To: {}", matchId, currentUserId, newOrganizerId);

        Match match = findMatchById(matchId);
        checkOrganizer(currentUserId, match);
        checkEditable(match);
        checkNotSelf(matchId, newOrganizerId, currentUserId);
        checkIsParticipant(match, newOrganizerId);

        match.setOrganizerId(newOrganizerId);
        log.info("Organizer transferred. MatchId: {}", matchId);

        return mapToResponse(match);
    }

    @Transactional
    @CacheEvict(value = "match:detail", key = "#matchId")
    public MatchResponse startMatch(UUID matchId, UUID currentUserId) {
        log.info("Initiating match start. MatchId: {}, RequesterId: {}", matchId, currentUserId);

        Match match = findMatchById(matchId);
        checkOrganizer(currentUserId, match);
        checkStartable(match);

        match.setStatus(Status.STARTED);
        Match saved = matchRepository.save(match);
        List<UUID> participantIds = matchParticipantService.getMatchParticipantIds(matchId);

        eventPublisher.publishEvent(new MatchStartedEvent(saved.getId(), saved.getOrganizerId(), participantIds, Instant.now()));

        log.info("Match started. MatchId: {}", matchId);
        return mapToResponse(match);
    }

    @Transactional
    @CacheEvict(value = "match:detail", key = "#matchId")
    public MatchResponse completeMatch(UUID matchId, UUID currentUserId, CompleteMatchReq req) {
        log.info("Initiating match completion. MatchId: {}, RequesterId: {}", matchId, currentUserId);

        Match match = findMatchById(matchId);
        checkOrganizer(currentUserId, match);
        checkCompletable(match);

        match.setStatus(Status.COMPLETED);
        match.setHomeScore(req.homeScore());
        match.setAwayScore(req.awayScore());
        match.setRatingDeadline(LocalDateTime.now().plusHours(12));

        Match saved = matchRepository.save(match);
        List<UUID> participantIds = matchParticipantService.getMatchParticipantIds(saved.getId());

        eventPublisher.publishEvent(
                new MatchCompletedEvent(
                        saved.getId(),
                        saved.getOrganizerId(),
                        saved.getHomeScore(),
                        saved.getAwayScore(),
                        participantIds,
                        Instant.now()
                )
        );

        log.info("Match completed. MatchId: {}", matchId);
        return mapToResponse(match);
    }

    /*
     * =========================
     * READ OPERATIONS
     * =========================
     */

    @Transactional(readOnly = true)
    public Page<MatchResponse> searchMatches(String title, String location, LocalDate date,
                                              Status status, Visibility visibility, boolean excludeJoined, Pageable pageable) {
        Pageable validated = validatedPageable(pageable);
        log.debug("Searching matches. Title: {}, Location: {}, Date: {}, Status: {}, Visibility: {}, ExcludeJoined: {}",
                title, location, date, status, visibility, excludeJoined);

        UUID excludeUserId = excludeJoined ? SecurityUtils.getCurrentUserId() : null;
        Specification<Match> spec = MatchSpecification.search(title, location, date, status, visibility, excludeUserId);
        return matchRepository.findAll(spec, validated).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "match:detail", key = "#matchId")
    public MatchResponse getMatch(UUID matchId) {
        log.debug("Fetching match. MatchId: {}", matchId);
        return mapToResponse(findMatchById(matchId));
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> getMyMatches(UUID userId, String title, int page, int size) {
        log.debug("Fetching matches for user. UserId: {}", userId);
        return matchParticipantService.findMyMatches(userId, title, page, size)
                .stream()
                .map(p -> mapToResponse(p.getMatch()))
                .toList();
    }

    /*
     * =========================
     * PACKAGE-LEVEL ACCESS
     * =========================
     */

    public Match findMatchById(UUID matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> {
                    log.warn("Match not found. MatchId: {}", matchId);
                    return new BusinessException(MatchErrorCode.MATCH_NOT_FOUND);
                });
    }

    public void checkIsParticipant(Match match, UUID userId) {
        if (!matchParticipantService.isJoined(match.getId(), userId)) {
            log.warn("Transfer target is not a participant. MatchId: {}, TargetId: {}", match.getId(), userId);
            throw new BusinessException(ParticipationErrorCode.NOT_JOINED);
        }
    }

    public void checkIsMatchOpen(Match match) {
        if (match.getStatus() != Status.OPEN && match.getStatus() != Status.CREATED) {
            throw new BusinessException(ParticipationErrorCode.MATCH_NOT_OPEN);
        }
    }

    public void setStatusToRatingClosed(Match match){
        match.setStatus(Status.RATINGS_CLOSED);
        matchRepository.save(match);
    }

    /*
     * =========================
     * PRIVATE — VALIDATIONS
     * =========================
     */

    private void checkOrganizer(UUID currentUserId, Match match) {
        if (!match.getOrganizerId().equals(currentUserId)) {
            log.warn("Unauthorized action. MatchId: {}, OrganizerId: {}, RequesterId: {}",
                    match.getId(), match.getOrganizerId(), currentUserId);
            throw new BusinessException(MatchErrorCode.UNAUTHORIZED);
        }
    }

    private void checkEditable(Match match) {
        if (match.getStatus() != Status.CREATED
                && match.getStatus() != Status.OPEN
                && match.getStatus() != Status.FULL) {
            log.warn("Match not editable. MatchId: {}, Status: {}", match.getId(), match.getStatus());
            throw new BusinessException(MatchErrorCode.MATCH_NOT_EDITABLE);
        }
    }

    private void checkCancellable(Match match) {
        if (match.getStatus() == Status.COMPLETED || match.getStatus() == Status.CANCELLED) {
            log.warn("Invalid cancel transition. MatchId: {}, Status: {}", match.getId(), match.getStatus());
            throw new BusinessException(MatchErrorCode.INVALID_STATUS_TRANSITION);
        }
    }

    private void checkStartable(Match match) {
        if (match.getStatus() != Status.OPEN && match.getStatus() != Status.FULL) {
            throw new BusinessException(MatchErrorCode.INVALID_STATUS_TRANSITION);
        }
    }

    private void checkCompletable(Match match) {
        if (match.getStatus() != Status.STARTED) {
            throw new BusinessException(MatchErrorCode.INVALID_STATUS_TRANSITION);
        }
    }

    private void checkNotSelf(UUID matchId, UUID newOrganizerId, UUID currentUserId) {
        if (newOrganizerId.equals(currentUserId)) {
            log.warn("Transfer to self rejected. MatchId: {}", matchId);
            throw new BusinessException(MatchErrorCode.INVALID_STATUS_TRANSITION);
        }
    }

    /*
     * =========================
     * PRIVATE — FIELD UPDATES
     * =========================
     */

    private List<String> detectChanges(UpdateMatchRequest request, Match match) {
        List<String> changes = new ArrayList<>();
        if (request.title() != null && !request.title().equals(match.getTitle()))
            changes.add("Başlık: " + truncate(request.title(), 30));
        if (request.location() != null && !request.location().equals(match.getLocation()))
            changes.add("Konum: " + truncate(request.location(), 30));
        if (request.startTime() != null && !request.startTime().equals(match.getStartTime()))
            changes.add("Tarih güncellendi");
        if (request.description() != null && !request.description().equals(match.getDescription()))
            changes.add("Açıklama güncellendi");
        if (request.visibility() != null && !request.visibility().equals(match.getVisibility()))
            changes.add("Görünürlük: " + request.visibility());
        return changes;
    }

    private String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }

    private boolean applyFieldUpdates(UpdateMatchRequest request, Match match) {
        boolean updated = false;
        updated |= setIfChanged(match::getTitle,       match::setTitle,       request.title());
        updated |= setIfChanged(match::getDescription, match::setDescription, request.description());
        updated |= setIfChanged(match::getLocation,    match::setLocation,    request.location());
        updated |= setIfChanged(match::getStartTime,   match::setStartTime,   request.startTime());
        updated |= setIfChanged(match::getVisibility,  match::setVisibility,  request.visibility());
        return updated;
    }

    private boolean applyCapacityUpdate(Match match, Integer newCapacity) {
        if (newCapacity == null || newCapacity.equals(match.getCapacity())) return false;

        if (newCapacity < match.getParticipantCount()) {
            log.warn("Invalid capacity. MatchId: {}, Requested: {}, Current participants: {}",
                    match.getId(), newCapacity, match.getParticipantCount());
            throw new BusinessException(MatchErrorCode.INVALID_MATCH_CAPACITY);
        }

        match.setCapacity(newCapacity);
        syncStatusAfterCapacityChange(match);
        return true;
    }

    private void syncStatusAfterCapacityChange(Match match) {
        if (match.getParticipantCount().equals(match.getCapacity())) {
            match.setStatus(Status.FULL);
        } else if (match.getStatus() == Status.FULL) {
            match.setStatus(Status.OPEN);
        }
    }

    private <T> boolean setIfChanged(Supplier<T> getter, Consumer<T> setter, T newValue) {
        if (newValue != null && !newValue.equals(getter.get())) {
            setter.accept(newValue);
            return true;
        }
        return false;
    }

    /*
     * =========================
     * PRIVATE — PAGINATION
     * =========================
     */

    private Pageable validatedPageable(Pageable pageable) {
        if (pageable.getSort().isUnsorted() || hasInvalidSortProperty(pageable)) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by("startTime").descending());
        }
        return pageable;
    }

    private boolean hasInvalidSortProperty(Pageable pageable) {
        try {
            return pageable.getSort().stream().anyMatch(order ->
                    order.getProperty() == null
                    || order.getProperty().isBlank()
                    || order.getProperty().equalsIgnoreCase("string")
            );
        } catch (Exception e) {
            return true;
        }
    }

    /*
     * =========================
     * PRIVATE — MAPPERS
     * =========================
     */

    private Match mapToEntity(CreateMatchRequest request, UUID organizerId) {
        return Match.builder()
                .title(request.title())
                .description(request.description())
                .location(request.location())
                .startTime(request.startTime())
                .visibility(request.visibility())
                .status(Status.CREATED)
                .organizerId(organizerId)
                .capacity(request.capacity())
                .participantCount(INITIAL_PARTICIPANT_COUNT)
                .build();
    }

    private MatchResponse mapToResponse(Match match) {
        return new MatchResponse(
                match.getId(),
                match.getTitle(),
                match.getDescription(),
                match.getLocation(),
                match.getStartTime(),
                match.getVisibility(),
                match.getStatus(),
                match.getOrganizerId(),
                match.getCapacity(),
                match.getParticipantCount(),
                match.getCreatedAt(),
                match.getWhiteTeam()
        );
    }
}
