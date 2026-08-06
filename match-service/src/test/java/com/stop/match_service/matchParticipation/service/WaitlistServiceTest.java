package com.stop.match_service.matchParticipation.service;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaitlistServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private WaitlistRepository waitlistRepository;

    @Mock
    private MatchParticipantRepository matchParticipantRepository;

    @InjectMocks
    private WaitlistService service;

    private Match match(UUID matchId, UUID organizerId, Status status) {
        return Match.builder()
                .id(matchId)
                .organizerId(organizerId)
                .status(status)
                .capacity(10)
                .participantCount(10)
                .startTime(LocalDateTime.now().plusDays(1))
                .build();
    }

    private WaitlistEntry entry(UUID id, Match match, UUID userId, WaitlistStatus status, int order) {
        return WaitlistEntry.builder()
                .id(id)
                .match(match)
                .userId(userId)
                .status(status)
                .sortOrder(order)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void joinWaitlist_matchFull_succeedsAndReturnsPosition() {
        UUID matchId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Match match = match(matchId, organizerId, Status.FULL);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchParticipantRepository.existsByMatchIdAndUserIdAndStatus(matchId, userId, ParticipantStatus.JOINED))
                .thenReturn(false);
        when(waitlistRepository.existsByMatchIdAndUserIdAndStatus(matchId, userId, WaitlistStatus.WAITING))
                .thenReturn(false);
        when(waitlistRepository.countByMatchIdAndStatus(matchId, WaitlistStatus.WAITING)).thenReturn(0L);
        when(waitlistRepository.findMaxSortOrder(matchId, WaitlistStatus.WAITING)).thenReturn(0);

        UUID savedId = UUID.randomUUID();
        when(waitlistRepository.save(any())).thenAnswer(inv -> {
            WaitlistEntry e = inv.getArgument(0);
            e.setId(savedId);
            return e;
        });
        WaitlistEntry saved = entry(savedId, match, userId, WaitlistStatus.WAITING, 1);
        when(waitlistRepository.findAllByMatchIdAndStatusOrderBySortOrderAsc(matchId, WaitlistStatus.WAITING))
                .thenReturn(List.of(saved));

        WaitlistEntryResponse response = service.joinWaitlist(new ParticipantRequestReq(matchId), userId);

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.status()).isEqualTo(WaitlistStatus.WAITING);
        assertThat(response.position()).isEqualTo(1);
    }

    @Test
    void joinWaitlist_matchNotFull_throwsMatchNotFull() {
        UUID matchId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Match match = match(matchId, organizerId, Status.OPEN);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> service.joinWaitlist(new ParticipantRequestReq(matchId), userId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ParticipationErrorCode.MATCH_NOT_FULL);

        verify(waitlistRepository, never()).save(any());
    }

    @Test
    void joinWaitlist_alreadyJoinedAsParticipant_throwsAlreadyJoined() {
        UUID matchId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Match match = match(matchId, organizerId, Status.FULL);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchParticipantRepository.existsByMatchIdAndUserIdAndStatus(matchId, userId, ParticipantStatus.JOINED))
                .thenReturn(true);

        assertThatThrownBy(() -> service.joinWaitlist(new ParticipantRequestReq(matchId), userId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ParticipationErrorCode.ALREADY_JOINED);
    }

    @Test
    void joinWaitlist_alreadyWaitlisted_throwsAlreadyWaitlisted() {
        UUID matchId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Match match = match(matchId, organizerId, Status.FULL);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchParticipantRepository.existsByMatchIdAndUserIdAndStatus(matchId, userId, ParticipantStatus.JOINED))
                .thenReturn(false);
        when(waitlistRepository.existsByMatchIdAndUserIdAndStatus(matchId, userId, WaitlistStatus.WAITING))
                .thenReturn(true);

        assertThatThrownBy(() -> service.joinWaitlist(new ParticipantRequestReq(matchId), userId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ParticipationErrorCode.ALREADY_WAITLISTED);
    }

    @Test
    void joinWaitlist_atMaxCapacity_throwsWaitlistFull() {
        UUID matchId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Match match = match(matchId, organizerId, Status.FULL);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchParticipantRepository.existsByMatchIdAndUserIdAndStatus(matchId, userId, ParticipantStatus.JOINED))
                .thenReturn(false);
        when(waitlistRepository.existsByMatchIdAndUserIdAndStatus(matchId, userId, WaitlistStatus.WAITING))
                .thenReturn(false);
        when(waitlistRepository.countByMatchIdAndStatus(matchId, WaitlistStatus.WAITING)).thenReturn(10L);

        assertThatThrownBy(() -> service.joinWaitlist(new ParticipantRequestReq(matchId), userId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ParticipationErrorCode.WAITLIST_FULL);
    }

    @Test
    void leaveWaitlist_existingWaitingEntry_setsCancelled() {
        UUID matchId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        WaitlistEntry entry = entry(UUID.randomUUID(), match(matchId, UUID.randomUUID(), Status.FULL), userId, WaitlistStatus.WAITING, 1);

        when(waitlistRepository.findByMatchIdAndUserIdAndStatus(matchId, userId, WaitlistStatus.WAITING))
                .thenReturn(Optional.of(entry));

        service.leaveWaitlist(new ParticipantRequestReq(matchId), userId);

        assertThat(entry.getStatus()).isEqualTo(WaitlistStatus.CANCELLED);
    }

    @Test
    void leaveWaitlist_noEntry_throwsNotWaitlisted() {
        UUID matchId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(waitlistRepository.findByMatchIdAndUserIdAndStatus(matchId, userId, WaitlistStatus.WAITING))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.leaveWaitlist(new ParticipantRequestReq(matchId), userId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ParticipationErrorCode.NOT_WAITLISTED);
    }

    @Test
    void getWaitlist_nonOrganizer_throwsUnauthorized() {
        UUID matchId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        Match match = match(matchId, organizerId, Status.FULL);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> service.getWaitlist(matchId, otherUser))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getWaitlist_organizer_returnsFifoOrderedListWithPositions() {
        UUID matchId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();
        Match match = match(matchId, organizerId, Status.FULL);
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(waitlistRepository.findAllByMatchIdAndStatusOrderBySortOrderAsc(matchId, WaitlistStatus.WAITING))
                .thenReturn(List.of(
                        entry(UUID.randomUUID(), match, userA, WaitlistStatus.WAITING, 1),
                        entry(UUID.randomUUID(), match, userB, WaitlistStatus.WAITING, 2)
                ));

        List<WaitlistEntryResponse> result = service.getWaitlist(matchId, organizerId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).userId()).isEqualTo(userA);
        assertThat(result.get(0).position()).isEqualTo(1);
        assertThat(result.get(1).userId()).isEqualTo(userB);
        assertThat(result.get(1).position()).isEqualTo(2);
    }

    @Test
    void popNextWaiting_hasWaitingEntries_flipsOldestToPromotedAndReturnsUserId() {
        UUID matchId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        WaitlistEntry oldest = entry(UUID.randomUUID(), match(matchId, UUID.randomUUID(), Status.FULL), userId, WaitlistStatus.WAITING, 1);

        when(waitlistRepository.findFirstByMatchIdAndStatusOrderBySortOrderAsc(matchId, WaitlistStatus.WAITING))
                .thenReturn(Optional.of(oldest));

        Optional<UUID> result = service.popNextWaiting(matchId);

        assertThat(result).contains(userId);
        assertThat(oldest.getStatus()).isEqualTo(WaitlistStatus.PROMOTED);
    }

    @Test
    void popNextWaiting_noWaitingEntries_returnsEmpty() {
        UUID matchId = UUID.randomUUID();

        when(waitlistRepository.findFirstByMatchIdAndStatusOrderBySortOrderAsc(matchId, WaitlistStatus.WAITING))
                .thenReturn(Optional.empty());

        assertThat(service.popNextWaiting(matchId)).isEmpty();
    }
}
