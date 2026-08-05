package com.stop.match_service.matchParticipation.service;

import com.stop.match_service.match.entity.Match;
import com.stop.match_service.match.entity.Status;
import com.stop.match_service.match.kafka.event.JerseyColorDecidedEvent;
import com.stop.match_service.match.repository.MatchRepository;
import com.stop.match_service.matchParticipation.dto.request.TeamAssignmentReq;
import com.stop.match_service.matchParticipation.entity.MatchParticipant;
import com.stop.match_service.matchParticipation.entity.ParticipantStatus;
import com.stop.match_service.matchParticipation.entity.TeamType;
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
}
