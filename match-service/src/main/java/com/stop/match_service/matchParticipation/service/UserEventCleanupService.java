package com.stop.match_service.matchParticipation.service;

import com.stop.match_service.matchParticipation.kafka.event.UserDeletedEvent;
import com.stop.match_service.match.entity.Match;
import com.stop.match_service.match.entity.Status;
import com.stop.match_service.match.repository.MatchRepository;
import com.stop.match_service.matchParticipation.entity.MatchParticipant;
import com.stop.match_service.matchParticipation.entity.ParticipantStatus;
import com.stop.match_service.matchParticipation.entity.ParticipationRequestEntity;
import com.stop.match_service.matchParticipation.entity.RequestStatus;
import com.stop.match_service.matchParticipation.repository.MatchParticipantRepository;
import com.stop.match_service.matchParticipation.repository.ParticipationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventCleanupService {

    private final MatchParticipantRepository matchParticipantRepository;
    private final ParticipationRequestRepository participationRequestRepository;
    private final MatchRepository matchRepository;

    @Transactional
    public void handleUserDeleted(UserDeletedEvent event) {
        UUID userId = event.userId();
        log.info("Processing UserDeleted cleanup for userId={}, time={}", userId, event.timestamp());

        cancelPendingRequests(userId);

        List<MatchParticipant> participations =
                matchParticipantRepository.findAllByUserIdAndStatus(userId, ParticipantStatus.JOINED);

        int removedCount = 0;
        for (MatchParticipant participant : participations) {
            Match match = participant.getMatch();
            if (match.getStatus() == Status.CANCELLED || match.getStatus() == Status.COMPLETED) {
                continue;
            }
            handleMatchRemoval(match, participant, userId);
            removedCount++;
        }

        log.info("UserDeleted cleanup complete for userId={} — removed from {} matches", userId, removedCount);
    }

    @Transactional
    public void handleUserSuspended(UUID userId) {
        log.info("Processing UserSuspended cleanup for userId={}", userId);

        cancelPendingRequests(userId);

        List<MatchParticipant> participations =
                matchParticipantRepository.findAllByUserIdAndStatus(userId, ParticipantStatus.JOINED);

        LocalDateTime now = LocalDateTime.now();
        int removedCount = 0;

        for (MatchParticipant participant : participations) {
            Match match = participant.getMatch();
            if (match.getStatus() == Status.CANCELLED
                    || match.getStatus() == Status.COMPLETED
                    || match.getStatus() == Status.STARTED
                    || match.getStartTime().isBefore(now)) {
                continue;
            }
            handleMatchRemoval(match, participant, userId);
            removedCount++;
        }

        log.info("UserSuspended cleanup complete for userId={} — removed from {} upcoming matches", userId, removedCount);
    }

    private void handleMatchRemoval(Match match, MatchParticipant participant, UUID userId) {
        if (userId.equals(match.getOrganizerId())) {
            handleOrganizerRemoval(match, userId);
        }

        participant.setStatus(ParticipantStatus.REMOVED);
        matchParticipantRepository.save(participant);

        if (match.getStatus() != Status.CANCELLED) {
            match.setParticipantCount(match.getParticipantCount() - 1);
            if (match.getStatus() == Status.FULL && match.getParticipantCount() < match.getCapacity()) {
                match.setStatus(Status.OPEN);
            }
            matchRepository.save(match);
        }
    }

    private void cancelPendingRequests(UUID userId) {
        List<ParticipationRequestEntity> pendingRequests =
                participationRequestRepository.findAllByUserIdAndStatus(userId, RequestStatus.PENDING);

        if (!pendingRequests.isEmpty()) {
            pendingRequests.forEach(req -> req.setStatus(RequestStatus.CANCELLED));
            participationRequestRepository.saveAll(pendingRequests);
            log.debug("Cancelled {} pending requests for userId={}", pendingRequests.size(), userId);
        }
    }

    private void handleOrganizerRemoval(Match match, UUID organizerId) {
        List<MatchParticipant> remaining = matchParticipantRepository
                .findAllByMatchIdAndStatus(match.getId(), ParticipantStatus.JOINED)
                .stream()
                .filter(p -> !p.getUserId().equals(organizerId))
                .sorted(Comparator.comparing(MatchParticipant::getJoinedAt))
                .toList();

        if (!remaining.isEmpty()) {
            UUID newOrganizerId = remaining.get(0).getUserId();
            match.setOrganizerId(newOrganizerId);
            log.info("Transferred organizer role for matchId={} from {} to {}",
                    match.getId(), organizerId, newOrganizerId);
        } else {
            match.setStatus(Status.CANCELLED);
            log.info("Cancelled matchId={} — no participants remain after organizer removal (userId={})",
                    match.getId(), organizerId);
        }
    }
}
