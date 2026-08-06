package com.stop.match_service.matchParticipation.service;

import com.stop.match_service.match.entity.Match;
import com.stop.match_service.match.entity.Status;
import com.stop.match_service.match.kafka.event.JerseyColorDecidedEvent;
import com.stop.match_service.match.repository.MatchRepository;
import com.stop.match_service.matchParticipation.dto.request.ParticipantRequestReq;
import com.stop.match_service.matchParticipation.dto.request.RemoveParticipationReq;
import com.stop.match_service.matchParticipation.dto.request.TeamAssignmentReq;
import com.stop.match_service.matchParticipation.entity.MatchParticipant;
import com.stop.match_service.matchParticipation.entity.ParticipantStatus;
import com.stop.match_service.matchParticipation.entity.TeamType;
import com.stop.match_service.matchParticipation.kafka.event.ParticipantJoinedEvent;
import com.stop.match_service.matchParticipation.kafka.event.WaitlistPromotedEvent;
import com.stop.match_service.matchParticipation.repository.MatchParticipantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchParticipantServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchParticipantRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private WaitlistService waitlistService;

    @InjectMocks
    private MatchParticipantService service;

    private Match openMatch(UUID matchId, UUID organizerId) {
        return Match.builder()
                .id(matchId)
                .organizerId(organizerId)
                .status(Status.OPEN)
                .startTime(LocalDateTime.now().plusDays(1))
                .build();
    }

    private MatchParticipant participant(Match match, UUID userId, TeamType team) {
        return MatchParticipant.builder()
                .match(match)
                .userId(userId)
                .status(ParticipantStatus.JOINED)
                .team(team)
                .build();
    }

    @Test
    void assignTeams_actualTeamChange_decidesColorsAndPublishesEvent() {
        UUID matchId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        Match match = openMatch(matchId, organizerId);

        // Previously unassigned (null team) - any real assignment counts as a change.
        List<MatchParticipant> joined = List.of(
                participant(match, userA, null),
                participant(match, userB, null)
        );

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(repository.findAllByMatchIdAndStatus(matchId, ParticipantStatus.JOINED)).thenReturn(joined);

        TeamAssignmentReq req = new TeamAssignmentReq(List.of(
                new TeamAssignmentReq.PlayerTeamEntry(userA, TeamType.HOME),
                new TeamAssignmentReq.PlayerTeamEntry(userB, TeamType.AWAY)
        ));

        service.assignTeams(matchId, req, organizerId);

        assertThat(match.getWhiteTeam()).isIn(TeamType.HOME, TeamType.AWAY);

        ArgumentCaptor<JerseyColorDecidedEvent> captor = ArgumentCaptor.forClass(JerseyColorDecidedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        JerseyColorDecidedEvent event = captor.getValue();

        assertThat(event.matchId()).isEqualTo(matchId);
        Set<UUID> allNotified = new HashSet<>(event.whiteTeamUserIds());
        allNotified.addAll(event.blackTeamUserIds());
        assertThat(allNotified).containsExactlyInAnyOrder(userA, userB);
        assertThat(event.whiteTeamUserIds()).doesNotContainAnyElementsOf(event.blackTeamUserIds());
    }

    @Test
    void assignTeams_identicalResubmission_doesNotRedecideOrPublish() {
        UUID matchId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        Match match = openMatch(matchId, organizerId);
        match.setWhiteTeam(TeamType.HOME);

        // Already assigned exactly as the incoming request will specify - no real change.
        List<MatchParticipant> joined = List.of(
                participant(match, userA, TeamType.HOME),
                participant(match, userB, TeamType.AWAY)
        );

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(repository.findAllByMatchIdAndStatus(matchId, ParticipantStatus.JOINED)).thenReturn(joined);

        TeamAssignmentReq req = new TeamAssignmentReq(List.of(
                new TeamAssignmentReq.PlayerTeamEntry(userA, TeamType.HOME),
                new TeamAssignmentReq.PlayerTeamEntry(userB, TeamType.AWAY)
        ));

        service.assignTeams(matchId, req, organizerId);

        assertThat(match.getWhiteTeam()).isEqualTo(TeamType.HOME);
        verify(eventPublisher, never()).publishEvent(any(JerseyColorDecidedEvent.class));
    }

    private Match fullMatch(UUID matchId, UUID organizerId, LocalDateTime startTime) {
        return Match.builder()
                .id(matchId)
                .organizerId(organizerId)
                .status(Status.FULL)
                .capacity(2)
                .participantCount(2)
                .startTime(startTime)
                .build();
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> publishedEventsOfType(Class<T> type) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.atLeastOnce()).publishEvent(captor.capture());
        return captor.getAllValues().stream()
                .filter(type::isInstance)
                .map(e -> (T) e)
                .toList();
    }

    @Test
    void leaveMatch_matchWasFull_andWaitlistHasEntry_promotesUserAndPublishesEvents() {
        UUID matchId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();
        UUID leavingUserId = UUID.randomUUID();
        UUID promotedUserId = UUID.randomUUID();
        Match match = fullMatch(matchId, organizerId, LocalDateTime.now().plusDays(1));
        MatchParticipant leaving = participant(match, leavingUserId, null);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(repository.findByMatchIdAndUserId(matchId, leavingUserId)).thenReturn(Optional.of(leaving));
        when(waitlistService.popNextWaiting(matchId)).thenReturn(Optional.of(promotedUserId));
        when(repository.findByMatchIdAndUserId(matchId, promotedUserId)).thenReturn(Optional.empty());

        service.leaveMatch(new ParticipantRequestReq(matchId), leavingUserId);

        assertThat(match.getParticipantCount()).isEqualTo(2); // decremented then re-incremented by promotion
        assertThat(match.getStatus()).isEqualTo(Status.FULL); // re-filled by the promoted user

        assertThat(publishedEventsOfType(ParticipantJoinedEvent.class))
                .anyMatch(e -> e.userId().equals(promotedUserId));
        assertThat(publishedEventsOfType(WaitlistPromotedEvent.class))
                .anyMatch(e -> e.matchId().equals(matchId) && e.userId().equals(promotedUserId));
    }

    @Test
    void leaveMatch_matchWasFull_noWaitlistEntries_staysOpenNoPromotionEvent() {
        UUID matchId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();
        UUID leavingUserId = UUID.randomUUID();
        Match match = fullMatch(matchId, organizerId, LocalDateTime.now().plusDays(1));
        MatchParticipant leaving = participant(match, leavingUserId, null);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(repository.findByMatchIdAndUserId(matchId, leavingUserId)).thenReturn(Optional.of(leaving));
        when(waitlistService.popNextWaiting(matchId)).thenReturn(Optional.empty());

        service.leaveMatch(new ParticipantRequestReq(matchId), leavingUserId);

        assertThat(match.getStatus()).isEqualTo(Status.OPEN);
        assertThat(publishedEventsOfType(WaitlistPromotedEvent.class)).isEmpty();
        assertThat(publishedEventsOfType(ParticipantJoinedEvent.class)).isEmpty();
    }

    @Test
    void leaveMatch_matchWasNotFull_waitlistNeverConsulted() {
        UUID matchId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();
        UUID leavingUserId = UUID.randomUUID();
        Match match = openMatch(matchId, organizerId);
        match.setCapacity(5);
        match.setParticipantCount(2);
        MatchParticipant leaving = participant(match, leavingUserId, null);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(repository.findByMatchIdAndUserId(matchId, leavingUserId)).thenReturn(Optional.of(leaving));

        service.leaveMatch(new ParticipantRequestReq(matchId), leavingUserId);

        verify(waitlistService, never()).popNextWaiting(any());
    }

    @Test
    void removePlayer_matchWasFull_promotesWaitlistEntry_evenOnMatchDay() {
        UUID matchId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();
        UUID removedUserId = UUID.randomUUID();
        UUID promotedUserId = UUID.randomUUID();
        // Within the 2h self-leave cutoff - removePlayer must still work, unlike leaveMatch.
        Match match = fullMatch(matchId, organizerId, LocalDateTime.now().plusMinutes(30));
        MatchParticipant removed = participant(match, removedUserId, null);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(repository.findByMatchIdAndUserId(matchId, removedUserId)).thenReturn(Optional.of(removed));
        when(waitlistService.popNextWaiting(matchId)).thenReturn(Optional.of(promotedUserId));
        when(repository.findByMatchIdAndUserId(matchId, promotedUserId)).thenReturn(Optional.empty());

        service.removePlayer(new RemoveParticipationReq(matchId, removedUserId), organizerId);

        assertThat(match.getStatus()).isEqualTo(Status.FULL);
        assertThat(publishedEventsOfType(WaitlistPromotedEvent.class))
                .anyMatch(e -> e.userId().equals(promotedUserId));
    }
}
