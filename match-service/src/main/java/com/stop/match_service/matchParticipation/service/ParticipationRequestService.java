package com.stop.match_service.matchParticipation.service;

import com.stop.match_service.common.error.ParticipationErrorCode;
import com.stop.match_service.common.exception.BusinessException;
import com.stop.match_service.match.entity.Match;
import com.stop.match_service.match.entity.Status;
import com.stop.match_service.match.entity.Visibility;
import com.stop.match_service.match.service.MatchService;
import com.stop.match_service.matchParticipation.dto.request.ApproveParticipationReq;
import com.stop.match_service.matchParticipation.dto.request.ParticipantRequestReq;
import com.stop.match_service.matchParticipation.dto.response.ParticipationRequestRes;
import com.stop.match_service.matchParticipation.entity.ParticipantStatus;
import com.stop.match_service.matchParticipation.entity.ParticipationRequestEntity;
import com.stop.match_service.matchParticipation.entity.RequestStatus;
import com.stop.match_service.matchParticipation.kafka.event.ParticipationRequestApprovedEvent;
import com.stop.match_service.matchParticipation.kafka.event.ParticipationRequestRejectedEvent;
import com.stop.match_service.matchParticipation.kafka.event.ParticipationRequestSentEvent;
import com.stop.match_service.matchParticipation.repository.MatchParticipantRepository;
import com.stop.match_service.matchParticipation.repository.ParticipationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationRequestService {

    private final MatchService matchService;
    private final ParticipationRequestRepository requestRepository;
    private final MatchParticipantService matchParticipantService;
    private final ApplicationEventPublisher eventPublisher;

    private static final int MAX_REJECTIONS = 3;

    /*
     * =====================
     * İSTEK GÖNDERME
     * =====================
     */

    @Transactional
    public ParticipationRequestRes sendRequest(ParticipantRequestReq req, UUID userId) {
        log.info("Participation request. matchId={} userId={}", req.matchId(), userId);

        Match match = matchService.findMatchById(req.matchId());

        checkMatchIsJoinable(match);
        checkUserEligibility(match, userId);

        RequestStatus initialStatus = (match.getVisibility() == Visibility.PUBLIC)
                ? RequestStatus.APPROVED
                : RequestStatus.PENDING;

        ParticipationRequestEntity saved = requestRepository.save(
                ParticipationRequestEntity.builder()
                        .match(match)
                        .userId(userId)
                        .status(initialStatus)
                        .build()
        );

        if (match.getVisibility() == Visibility.PUBLIC) {
            matchParticipantService.join(match, userId);
            log.info("Public match auto-joined. matchId={} userId={}", match.getId(), userId);
        } else {
            log.info("Private match request pending. requestId={}", saved.getId());
            eventPublisher.publishEvent(new ParticipationRequestSentEvent(
                    saved.getId(),
                    match.getId(),
                    userId,
                    match.getOrganizerId(),
                    match.getTitle(),
                    Instant.now()
            ));
        }

        return toResponse(saved);
    }

    /*
     * =====================
     * ONAYLAMA / REDDETME
     * =====================
     */

    @Transactional
    public ParticipationRequestRes approveRequest(ApproveParticipationReq req, UUID organizerId) {
        log.info("Approving participation. requestId={} organizerId={}", req.id(), organizerId);

        ParticipationRequestEntity request = getRequestById(req.id());
        Match match = request.getMatch();

        matchParticipantService.checkIsOrganizer(match, organizerId);
        checkRequestIsPending(request);
        checkMatchCapacity(match);

        request.setStatus(RequestStatus.APPROVED);
        matchParticipantService.join(match, request.getUserId());

        eventPublisher.publishEvent(new ParticipationRequestApprovedEvent(
                request.getId(),
                match.getId(),
                request.getUserId(),
                match.getTitle(),
                Instant.now()
        ));

        log.info("Participation approved. requestId={} matchId={} userId={}",
                req.id(), match.getId(), request.getUserId());
        return toResponse(request);
    }

    @Transactional
    public ParticipationRequestRes rejectRequest(UUID requestId, UUID organizerId) {
        log.info("Rejecting participation. requestId={} organizerId={}", requestId, organizerId);

        ParticipationRequestEntity request = getRequestById(requestId);
        matchParticipantService.checkIsOrganizer(request.getMatch(), organizerId);
        checkRequestIsPending(request);

        request.setStatus(RequestStatus.REJECTED);

        eventPublisher.publishEvent(new ParticipationRequestRejectedEvent(
                request.getId(),
                request.getMatch().getId(),
                request.getUserId(),
                request.getMatch().getTitle(),
                Instant.now()
        ));

        log.info("Participation rejected. requestId={}", requestId);
        return toResponse(request);
    }

    @Transactional
    public void withdrawRequest(UUID requestId, UUID userId) {
        log.info("Withdrawing participation request. requestId={} userId={}", requestId, userId);

        ParticipationRequestEntity request = getRequestByIdAndUserId(requestId, userId);
        checkRequestIsPending(request);

        request.setStatus(RequestStatus.CANCELLED);
        log.info("Participation request withdrawn. requestId={}", requestId);
    }

    /*
     * =====================
     * SORGULAR
     * =====================
     */

    @Transactional(readOnly = true)
    public List<ParticipationRequestRes> getPendingRequests(UUID matchId, UUID organizerId) {
        Match match = matchService.findMatchById(matchId);
        matchParticipantService.checkIsOrganizer(match, organizerId);
        return requestRepository.findAllByMatchIdAndStatus(matchId, RequestStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ParticipationRequestRes getMyRequest(UUID matchId, UUID userId) {
        return requestRepository
                .findTopByMatchIdAndUserIdOrderByCreatedAtDesc(matchId, userId)
                .map(this::toResponse)
                .orElse(null);
    }

    /*
     * =====================
     * PRIVATE HELPERS
     * =====================
     */

    private void checkMatchIsJoinable(Match match) {
        if (match.getStatus() != Status.OPEN && match.getStatus() != Status.CREATED) {
            throw new BusinessException(ParticipationErrorCode.MATCH_NOT_OPEN);
        }
        checkMatchCapacity(match);
    }

    private void checkMatchCapacity(Match match) {
        if (match.getParticipantCount() >= match.getCapacity()) {
            throw new BusinessException(ParticipationErrorCode.MATCH_FULL);
        }
    }

    private void checkUserEligibility(Match match, UUID userId) {
        if (matchParticipantService.isJoined(match.getId(), userId)) {
            throw new BusinessException(ParticipationErrorCode.ALREADY_JOINED);
        }
        if (requestRepository.existsByMatchIdAndUserIdAndStatus(match.getId(), userId, RequestStatus.PENDING)) {
            throw new BusinessException(ParticipationErrorCode.ALREADY_REQUESTED);
        }
        long rejections = requestRepository.countByMatchIdAndUserIdAndStatus(
                match.getId(), userId, RequestStatus.REJECTED);
        if (rejections >= MAX_REJECTIONS) {
            throw new BusinessException(ParticipationErrorCode.MAX_REJECTIONS_REACHED);
        }
    }

    private void checkRequestIsPending(ParticipationRequestEntity request) {
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BusinessException(ParticipationErrorCode.INVALID_REQUEST_STATUS);
        }
    }

    private ParticipationRequestEntity getRequestById(UUID requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ParticipationErrorCode.REQUEST_NOT_FOUND));
    }

    private ParticipationRequestEntity getRequestByIdAndUserId(UUID requestId, UUID userId) {
        return requestRepository.findByIdAndUserId(requestId, userId)
                .orElseThrow(() -> new BusinessException(ParticipationErrorCode.REQUEST_NOT_FOUND));
    }

    private ParticipationRequestRes toResponse(ParticipationRequestEntity e) {
        return new ParticipationRequestRes(
                e.getId(),
                e.getUserId(),
                e.getMatch().getId(),
                e.getStatus()
        );
    }
}
