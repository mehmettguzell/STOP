package com.stop.match_service.invitation.service;

import com.stop.match_service.common.dto.response.SliceResponse;
import com.stop.match_service.common.error.InvitationErrorCode;
import com.stop.match_service.common.error.MatchErrorCode;
import com.stop.match_service.common.exception.BusinessException;
import com.stop.match_service.invitation.dto.request.UpdateInvitationRequest;
import com.stop.match_service.invitation.dto.request.InvitationRequest;
import com.stop.match_service.invitation.dto.response.InvitationResponse;
import com.stop.match_service.invitation.entity.Invitation;
import com.stop.match_service.invitation.entity.InvitationStatus;
import com.stop.match_service.invitation.kafka.event.InvitationSentEvent;
import com.stop.match_service.match.entity.Match;
import com.stop.match_service.match.entity.Status;
import com.stop.match_service.invitation.repository.InvitationRepository;
import com.stop.match_service.match.service.MatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.print.Pageable;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final MatchService matchService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public InvitationResponse createInvitation(InvitationRequest request, UUID senderId) {
        validateInvitationRequest(senderId, request.receiverId(), request.matchId());
        Invitation invitation = toEntity(request, senderId);

        try {
            invitationRepository.save(invitation);
            eventPublisher.publishEvent(new InvitationSentEvent(
                    invitation.getId(), invitation.getMatchId(),
                    senderId, invitation.getReceiverId(), java.time.Instant.now()
            ));
            return mapToResponse(invitation);
        } catch (DataIntegrityViolationException e) {
            log.error("Duplicate invitation detected in DB: matchId={}, receiverId={}",
                    request.matchId(), request.receiverId());
            throw new BusinessException(InvitationErrorCode.PENDING_INVITATION_EXISTS);
        }
    }

    @Transactional
    public InvitationResponse acceptInvitation(UpdateInvitationRequest request, UUID currentUserId) {
        Invitation invitation = getProcessableInvitation(request.matchId(), currentUserId);
        invitation.setStatus(InvitationStatus.ACCEPTED);
        return mapToResponse(invitationRepository.save(invitation));
    }

    @Transactional
    public InvitationResponse rejectInvitation(UpdateInvitationRequest request, UUID currentUserId) {
        Invitation invitation = getProcessableInvitation(request.matchId(), currentUserId);
        invitation.setStatus(InvitationStatus.REJECTED);
        return mapToResponse(invitationRepository.save(invitation));
    }

    @Transactional(readOnly = true)
    public SliceResponse<InvitationResponse> getAllInvitations(UUID currentUserId, int page, int size) {
        return SliceResponse.of(
                invitationRepository.findAllByReceiverIdAndStatus(
                        currentUserId,
                        InvitationStatus.PENDING,
                        PageRequest.of(page,size)
                    ).map(this::mapToResponse));
    }

    // ===== private Helpers

    private Invitation getProcessableInvitation(UUID matchId, UUID currentUserId) {
        validateMatchAvailability(matchId);

        Invitation invitation = invitationRepository
                .findByReceiverIdAndMatchId(currentUserId, matchId)
                .orElseThrow(() -> new BusinessException(InvitationErrorCode.INVITATION_NOT_FOUND));

        validateInvitationIsPending(invitation);

        return invitation;
    }

    private void validateMatchAvailability(UUID matchId) {
        Match match = matchService.findMatchById(matchId);
        Status matchStatus = match.getStatus();

        if (matchStatus != Status.CREATED && matchStatus != Status.OPEN) {
            throw new BusinessException(MatchErrorCode.MATCH_NOT_AVAILABLE);
        }
    }

    private void validateInvitationIsPending(Invitation invitation) {
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BusinessException(InvitationErrorCode.INVITATION_ALREADY_PROCESSED);
        }
    }

    private void validateInvitationRequest(UUID senderId, UUID receiverId, UUID matchId) {
        if (senderId.equals(receiverId)) {
            throw new BusinessException(InvitationErrorCode.INVALID_INVITATION);
        }

        boolean exists = invitationRepository.existsByMatchIdAndReceiverIdAndStatus(
                matchId, receiverId, InvitationStatus.PENDING);

        if (exists) {
            throw new BusinessException(InvitationErrorCode.PENDING_INVITATION_EXISTS);
        }
    }

    private Invitation toEntity(InvitationRequest request, UUID senderId) {
        return Invitation.builder()
                .matchId(request.matchId())
                .senderId(senderId)
                .receiverId(request.receiverId())
                .status(InvitationStatus.PENDING)
                .build();
    }

    private InvitationResponse mapToResponse(Invitation invitation) {
        return new InvitationResponse(
                invitation.getMatchId(),
                invitation.getSenderId(),
                invitation.getReceiverId(),
                invitation.getStatus()
        );
    }



}