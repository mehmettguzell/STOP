package com.stop.match_service.matchParticipation.service;

import com.stop.match_service.common.error.MatchErrorCode;
import com.stop.match_service.common.error.ParticipationErrorCode;
import com.stop.match_service.common.exception.BusinessException;
import com.stop.match_service.match.entity.Match;
import com.stop.match_service.match.entity.Status;
import com.stop.match_service.match.repository.MatchRepository;
import com.stop.match_service.matchParticipation.dto.request.ParticipantRequestReq;
import com.stop.match_service.matchParticipation.dto.response.WaitlistEntryResponse;
import com.stop.match_service.matchParticipation.entity.ParticipantStatus;
import com.stop.match_service.matchParticipation.entity.WaitlistEntry;
import com.stop.match_service.matchParticipation.entity.WaitlistStatus;
import com.stop.match_service.matchParticipation.repository.MatchParticipantRepository;
import com.stop.match_service.matchParticipation.repository.WaitlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Manages the per-match waitlist (max {@link #MAX_WAITLIST_SIZE} entries). Deliberately has no
 * dependency on MatchParticipantService/MatchService - MatchParticipantService depends on this
 * class (to trigger promotion when a slot frees up), and the reverse dependency would create a
 * circular bean graph. Match lookups and organizer checks are therefore duplicated locally
 * rather than reused from those services, matching this codebase's existing style of small,
 * per-class private guards.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WaitlistService {

    private static final int MAX_WAITLIST_SIZE = 10;

    private final MatchRepository matchRepository;
    private final WaitlistRepository waitlistRepository;
    private final MatchParticipantRepository matchParticipantRepository;

    @Transactional
    public WaitlistEntryResponse joinWaitlist(ParticipantRequestReq req, UUID userId) {
        Match match = findMatchById(req.matchId());
        checkMatchIsFull(match);
        checkUserEligibility(match, userId);

        int nextOrder = waitlistRepository.findMaxSortOrder(match.getId(), WaitlistStatus.WAITING) + 1;
        WaitlistEntry saved = waitlistRepository.save(WaitlistEntry.builder()
                .match(match)
                .userId(userId)
                .status(WaitlistStatus.WAITING)
                .sortOrder(nextOrder)
                .build());

        log.info("User joined waitlist. matchId={} userId={}", match.getId(), userId);
        return toResponse(saved, computeRank(match.getId(), saved.getId()));
    }

    @Transactional
    public void leaveWaitlist(ParticipantRequestReq req, UUID userId) {
        WaitlistEntry entry = waitlistRepository
                .findByMatchIdAndUserIdAndStatus(req.matchId(), userId, WaitlistStatus.WAITING)
                .orElseThrow(() -> new BusinessException(ParticipationErrorCode.NOT_WAITLISTED));
        entry.setStatus(WaitlistStatus.CANCELLED);
        log.info("User left waitlist. matchId={} userId={}", req.matchId(), userId);
    }

    @Transactional
    public void removeFromWaitlist(UUID matchId, UUID targetUserId, UUID organizerId) {
        Match match = findMatchById(matchId);
        checkIsOrganizer(match, organizerId);

        WaitlistEntry entry = waitlistRepository
                .findByMatchIdAndUserIdAndStatus(matchId, targetUserId, WaitlistStatus.WAITING)
                .orElseThrow(() -> new BusinessException(ParticipationErrorCode.NOT_WAITLISTED));
        entry.setStatus(WaitlistStatus.CANCELLED);
        log.info("Organizer removed user from waitlist. matchId={} targetUserId={} organizerId={}",
                matchId, targetUserId, organizerId);
    }

    @Transactional
    public List<WaitlistEntryResponse> reorderWaitlist(UUID matchId, List<UUID> orderedEntryIds, UUID organizerId) {
        Match match = findMatchById(matchId);
        checkIsOrganizer(match, organizerId);

        List<WaitlistEntry> waiting = waitlistRepository.findAllByMatchIdAndStatusOrderBySortOrderAsc(matchId, WaitlistStatus.WAITING);

        Set<UUID> currentIds = waiting.stream().map(WaitlistEntry::getId).collect(Collectors.toSet());
        Set<UUID> requestedIds = Set.copyOf(orderedEntryIds);
        if (!currentIds.equals(requestedIds) || orderedEntryIds.size() != waiting.size()) {
            throw new BusinessException(ParticipationErrorCode.INVALID_WAITLIST_REORDER);
        }

        var byId = waiting.stream().collect(Collectors.toMap(WaitlistEntry::getId, e -> e));
        for (int i = 0; i < orderedEntryIds.size(); i++) {
            byId.get(orderedEntryIds.get(i)).setSortOrder(i + 1);
        }

        log.info("Waitlist reordered. matchId={} organizerId={}", matchId, organizerId);
        List<WaitlistEntry> reordered = waitlistRepository.findAllByMatchIdAndStatusOrderBySortOrderAsc(matchId, WaitlistStatus.WAITING);
        return indexed(reordered);
    }

    @Transactional(readOnly = true)
    public List<WaitlistEntryResponse> getWaitlist(UUID matchId, UUID organizerId) {
        Match match = findMatchById(matchId);
        checkIsOrganizer(match, organizerId);
        List<WaitlistEntry> waiting = waitlistRepository.findAllByMatchIdAndStatusOrderBySortOrderAsc(matchId, WaitlistStatus.WAITING);
        return indexed(waiting);
    }

    @Transactional(readOnly = true)
    public WaitlistEntryResponse getMyWaitlistEntry(UUID matchId, UUID userId) {
        return waitlistRepository.findTopByMatchIdAndUserIdOrderByCreatedAtDesc(matchId, userId)
                .map(e -> toResponse(e, e.getStatus() == WaitlistStatus.WAITING ? computeRank(matchId, e.getId()) : null))
                .orElse(null);
    }

    /**
     * Narrow, one-directional hook called by MatchParticipantService when a confirmed slot
     * frees up. Deliberately does NOT call back into MatchParticipantService/MatchService (would
     * recreate a circular bean dependency) - it only flips the oldest WAITING row to PROMOTED and
     * returns the userId; the caller is responsible for actually re-joining that user.
     */
    @Transactional
    public Optional<UUID> popNextWaiting(UUID matchId) {
        return waitlistRepository.findFirstByMatchIdAndStatusOrderBySortOrderAsc(matchId, WaitlistStatus.WAITING)
                .map(entry -> {
                    entry.setStatus(WaitlistStatus.PROMOTED);
                    log.info("Waitlist entry promoted. matchId={} userId={}", matchId, entry.getUserId());
                    return entry.getUserId();
                });
    }

    /*
     * =====================
     * PRIVATE HELPERS
     * =====================
     */

    private void checkMatchIsFull(Match match) {
        if (match.getStatus() != Status.FULL) {
            throw new BusinessException(ParticipationErrorCode.MATCH_NOT_FULL);
        }
    }

    private void checkUserEligibility(Match match, UUID userId) {
        if (matchParticipantRepository.existsByMatchIdAndUserIdAndStatus(match.getId(), userId, ParticipantStatus.JOINED)) {
            throw new BusinessException(ParticipationErrorCode.ALREADY_JOINED);
        }
        if (waitlistRepository.existsByMatchIdAndUserIdAndStatus(match.getId(), userId, WaitlistStatus.WAITING)) {
            throw new BusinessException(ParticipationErrorCode.ALREADY_WAITLISTED);
        }
        long waitingCount = waitlistRepository.countByMatchIdAndStatus(match.getId(), WaitlistStatus.WAITING);
        if (waitingCount >= MAX_WAITLIST_SIZE) {
            throw new BusinessException(ParticipationErrorCode.WAITLIST_FULL);
        }
    }

    // Duplicated deliberately (not injected from MatchParticipantService) - see class Javadoc.
    private void checkIsOrganizer(Match match, UUID userId) {
        if (!match.getOrganizerId().equals(userId)) {
            log.warn("Unauthorized waitlist action. matchId={} expectedOrganizer={} actualUser={}",
                    match.getId(), match.getOrganizerId(), userId);
            throw new BusinessException(MatchErrorCode.UNAUTHORIZED);
        }
    }

    private Match findMatchById(UUID matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(MatchErrorCode.MATCH_NOT_FOUND));
    }

    private Integer computeRank(UUID matchId, UUID entryId) {
        List<WaitlistEntry> waiting = waitlistRepository.findAllByMatchIdAndStatusOrderBySortOrderAsc(matchId, WaitlistStatus.WAITING);
        for (int i = 0; i < waiting.size(); i++) {
            if (waiting.get(i).getId().equals(entryId)) {
                return i + 1;
            }
        }
        return null;
    }

    private List<WaitlistEntryResponse> indexed(List<WaitlistEntry> waiting) {
        return IntStream.range(0, waiting.size())
                .mapToObj(i -> toResponse(waiting.get(i), i + 1))
                .toList();
    }

    private WaitlistEntryResponse toResponse(WaitlistEntry e, Integer rank) {
        return new WaitlistEntryResponse(e.getId(), e.getMatch().getId(), e.getUserId(), e.getStatus(), e.getCreatedAt(), rank);
    }
}
