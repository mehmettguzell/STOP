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
import com.stop.match_service.matchParticipation.service.MatchParticipantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchParticipantService matchParticipantService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MatchService matchService;

    private UUID organizerId;
    private UUID matchId;
    private Match openMatch;

    @BeforeEach
    void setUp() {
        organizerId = UUID.randomUUID();
        matchId = UUID.randomUUID();

        openMatch = Match.builder()
                .id(matchId)
                .title("Test Maçı")
                .description("Test açıklama")
                .location("İstanbul")
                .startTime(LocalDateTime.now().plusDays(1))
                .visibility(Visibility.PUBLIC)
                .status(Status.OPEN)
                .organizerId(organizerId)
                .capacity(10)
                .participantCount(3)
                .build();
    }

    // ==================== createMatch ====================

    @Test
    void createMatch_savesAndAddsInitialParticipant() {
        CreateMatchRequest req = new CreateMatchRequest(
                "Maç 1", "Açıklama", "Kadıköy",
                LocalDateTime.now().plusDays(1), Visibility.PUBLIC, 10
        );

        Match savedMatch = Match.builder()
                .id(UUID.randomUUID())
                .title(req.title())
                .description(req.description())
                .location(req.location())
                .startTime(req.startTime())
                .visibility(req.visibility())
                .status(Status.CREATED)
                .organizerId(organizerId)
                .capacity(req.capacity())
                .participantCount(1)
                .createdAt(LocalDateTime.now())
                .build();

        when(matchRepository.save(any(Match.class))).thenReturn(savedMatch);

        MatchResponse response = matchService.createMatch(req, organizerId);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Maç 1");
        verify(matchRepository).save(any(Match.class));
        verify(matchParticipantService).addInitialParticipant(any(Match.class), eq(organizerId));
    }

    // ==================== cancelMatch ====================

    @Test
    void cancelMatch_happyPath_deletesMatchAndPublishesEvent() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(openMatch));
        when(matchParticipantService.getMatchParticipantIds(matchId)).thenReturn(List.of(organizerId));

        matchService.cancelMatch(matchId, organizerId);

        ArgumentCaptor<MatchCancelledEvent> eventCaptor = ArgumentCaptor.forClass(MatchCancelledEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().matchId()).isEqualTo(matchId);
        verify(matchRepository).deleteById(matchId);
        verify(matchRepository, never()).save(any());
    }

    @Test
    void cancelMatch_wrongOrganizer_throwsUnauthorized() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(openMatch));

        assertThatThrownBy(() -> matchService.cancelMatch(matchId, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(MatchErrorCode.UNAUTHORIZED));
    }

    @Test
    void cancelMatch_alreadyCancelled_throwsInvalidTransition() {
        openMatch.setStatus(Status.CANCELLED);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(openMatch));

        assertThatThrownBy(() -> matchService.cancelMatch(matchId, organizerId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(MatchErrorCode.INVALID_STATUS_TRANSITION));
    }

    @Test
    void cancelMatch_alreadyCompleted_throwsInvalidTransition() {
        openMatch.setStatus(Status.COMPLETED);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(openMatch));

        assertThatThrownBy(() -> matchService.cancelMatch(matchId, organizerId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(MatchErrorCode.INVALID_STATUS_TRANSITION));
    }

    // ==================== startMatch ====================

    @Test
    void startMatch_fromOpen_setsStartedAndPublishesEvent() {
        openMatch.setStatus(Status.OPEN);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(openMatch));
        when(matchRepository.save(any())).thenReturn(openMatch);
        when(matchParticipantService.getMatchParticipantIds(matchId)).thenReturn(List.of(organizerId));

        MatchResponse response = matchService.startMatch(matchId, organizerId);

        assertThat(response.status()).isEqualTo(Status.STARTED);
        ArgumentCaptor<MatchStartedEvent> captor = ArgumentCaptor.forClass(MatchStartedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().matchId()).isEqualTo(matchId);
    }

    @Test
    void startMatch_fromFull_setsStartedAndPublishesEvent() {
        openMatch.setStatus(Status.FULL);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(openMatch));
        when(matchRepository.save(any())).thenReturn(openMatch);
        when(matchParticipantService.getMatchParticipantIds(matchId)).thenReturn(List.of(organizerId));

        MatchResponse response = matchService.startMatch(matchId, organizerId);

        assertThat(response.status()).isEqualTo(Status.STARTED);
    }

    @Test
    void startMatch_fromCreated_throwsInvalidTransition() {
        openMatch.setStatus(Status.CREATED);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(openMatch));

        assertThatThrownBy(() -> matchService.startMatch(matchId, organizerId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(MatchErrorCode.INVALID_STATUS_TRANSITION));
    }

    @Test
    void startMatch_wrongOrganizer_throwsUnauthorized() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(openMatch));

        assertThatThrownBy(() -> matchService.startMatch(matchId, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(MatchErrorCode.UNAUTHORIZED));
    }

    // ==================== completeMatch ====================

    @Test
    void completeMatch_fromStarted_setsCompletedAndPublishesEvent() {
        openMatch.setStatus(Status.STARTED);
        CompleteMatchReq req = new CompleteMatchReq(2, 1);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(openMatch));
        when(matchRepository.save(any())).thenReturn(openMatch);
        when(matchParticipantService.getMatchParticipantIds(matchId)).thenReturn(List.of(organizerId));

        MatchResponse response = matchService.completeMatch(matchId, organizerId, req);

        assertThat(response.status()).isEqualTo(Status.COMPLETED);
        ArgumentCaptor<MatchCompletedEvent> captor = ArgumentCaptor.forClass(MatchCompletedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().homeScore()).isEqualTo(2);
        assertThat(captor.getValue().awayScore()).isEqualTo(1);
    }

    @Test
    void completeMatch_notStarted_throwsInvalidTransition() {
        openMatch.setStatus(Status.OPEN);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(openMatch));

        assertThatThrownBy(() -> matchService.completeMatch(matchId, organizerId, new CompleteMatchReq(1, 0)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(MatchErrorCode.INVALID_STATUS_TRANSITION));
    }

    // ==================== transferOrganizer ====================

    @Test
    void transferOrganizer_happyPath_changesOrganizer() {
        UUID newOrganizerId = UUID.randomUUID();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(openMatch));
        when(matchParticipantService.isJoined(matchId, newOrganizerId)).thenReturn(true);

        MatchResponse response = matchService.transferOrganizer(matchId, newOrganizerId, organizerId);

        assertThat(response.organizerId()).isEqualTo(newOrganizerId);
    }

    @Test
    void transferOrganizer_targetNotParticipant_throwsNotJoined() {
        UUID newOrganizerId = UUID.randomUUID();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(openMatch));
        when(matchParticipantService.isJoined(matchId, newOrganizerId)).thenReturn(false);

        assertThatThrownBy(() -> matchService.transferOrganizer(matchId, newOrganizerId, organizerId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ParticipationErrorCode.NOT_JOINED));
    }

    @Test
    void transferOrganizer_selfTransfer_throwsException() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(openMatch));

        assertThatThrownBy(() -> matchService.transferOrganizer(matchId, organizerId, organizerId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void transferOrganizer_notOrganizer_throwsUnauthorized() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(openMatch));

        assertThatThrownBy(() -> matchService.transferOrganizer(matchId, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(MatchErrorCode.UNAUTHORIZED));
    }

    // ==================== updateMatch ====================

    @Test
    void updateMatch_capacityBelowParticipantCount_throwsInvalidCapacity() {
        openMatch.setParticipantCount(5);
        UpdateMatchRequest req = new UpdateMatchRequest(null, null, null, null, null, 3);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(openMatch));

        assertThatThrownBy(() -> matchService.updateMatch(req, matchId, organizerId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(MatchErrorCode.INVALID_MATCH_CAPACITY));
    }

    @Test
    void updateMatch_titleChange_publishesUpdatedEvent() {
        UpdateMatchRequest req = new UpdateMatchRequest("Yeni Başlık", null, null, null, null, null);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(openMatch));
        when(matchParticipantService.getMatchParticipantIds(matchId)).thenReturn(List.of(organizerId));

        matchService.updateMatch(req, matchId, organizerId);

        verify(eventPublisher).publishEvent(any(MatchUpdatedEvent.class));
    }

    @Test
    void updateMatch_notEditable_throwsMatchNotEditable() {
        openMatch.setStatus(Status.STARTED);
        UpdateMatchRequest req = new UpdateMatchRequest("Yeni Başlık", null, null, null, null, null);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(openMatch));

        assertThatThrownBy(() -> matchService.updateMatch(req, matchId, organizerId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(MatchErrorCode.MATCH_NOT_EDITABLE));
    }

    // ==================== getMatch ====================

    @Test
    void getMatch_notFound_throwsMatchNotFound() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.getMatch(matchId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(MatchErrorCode.MATCH_NOT_FOUND));
    }

    @Test
    void getMatch_found_returnsResponse() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(openMatch));

        MatchResponse response = matchService.getMatch(matchId);

        assertThat(response.id()).isEqualTo(matchId);
        assertThat(response.title()).isEqualTo("Test Maçı");
    }

    // ==================== setStatusToRatingClosed ====================

    @Test
    void setStatusToRatingClosed_updatesStatusAndSaves() {
        openMatch.setStatus(Status.COMPLETED);
        when(matchRepository.save(any())).thenReturn(openMatch);

        matchService.setStatusToRatingClosed(openMatch);

        assertThat(openMatch.getStatus()).isEqualTo(Status.RATINGS_CLOSED);
        verify(matchRepository).save(openMatch);
    }
}
