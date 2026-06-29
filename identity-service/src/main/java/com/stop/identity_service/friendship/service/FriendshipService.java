package com.stop.identity_service.friendship.service;

import com.stop.identity_service.common.error.IdentityErrorCode;
import com.stop.identity_service.common.exception.BusinessException;
import com.stop.identity_service.friendship.dto.response.FriendshipResponse;
import com.stop.identity_service.friendship.dto.response.SliceResponse;
import com.stop.identity_service.friendship.entity.Friendship;
import com.stop.identity_service.friendship.entity.FriendshipStatus;
import com.stop.identity_service.friendship.kafka.event.FriendRequestAcceptedEvent;
import com.stop.identity_service.friendship.kafka.event.FriendRequestSentEvent;
import com.stop.identity_service.friendship.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipRepository repository;
    private final ApplicationEventPublisher eventPublisher;


    // ─── WRITE ────────────────────────────────────────────────────────────────

    @Transactional
    public FriendshipResponse sendFriendshipRequest(UUID requesterId, UUID receiverId) {
        if (requesterId.equals(receiverId)) {
            throw new BusinessException(IdentityErrorCode.CANNOT_FRIEND_YOURSELF);
        }
        if (repository.existsPair(requesterId, receiverId)) {
            throw new BusinessException(IdentityErrorCode.FRIENDSHIP_ALREADY_EXISTS);
        }

        Friendship friendship = repository.save(Friendship.builder()
                .requesterId(requesterId)
                .receiverId(receiverId)
                .status(FriendshipStatus.PENDING)
                .build());

        eventPublisher.publishEvent(new FriendRequestSentEvent(
                friendship.getId(), requesterId, receiverId, friendship.getCreatedAt()));

        return toResponse(friendship);
    }

    @Transactional
    public FriendshipResponse acceptFriendshipRequest(UUID friendshipId, UUID currentUserId) {
        Friendship friendship = findAndValidateReceiver(friendshipId, currentUserId);
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendship = repository.save(friendship);

        eventPublisher.publishEvent(new FriendRequestAcceptedEvent(
                friendship.getId(), friendship.getRequesterId(), currentUserId, Instant.now()));

        return toResponse(friendship);
    }

    @Transactional
    public FriendshipResponse rejectFriendshipRequest(UUID friendshipId, UUID currentUserId) {
        Friendship friendship = findAndValidateReceiver(friendshipId, currentUserId);
        friendship.setStatus(FriendshipStatus.REJECTED);
        return toResponse(repository.save(friendship));
    }

    @Transactional
    public void cancelFriendshipRequest(UUID friendshipId, UUID currentUserId) {
        Friendship friendship = repository.findById(friendshipId)
                .orElseThrow(() -> new BusinessException(IdentityErrorCode.FRIENDSHIP_NOT_FOUND));
        if (!friendship.getRequesterId().equals(currentUserId)) {
            throw new BusinessException(IdentityErrorCode.FRIENDSHIP_FORBIDDEN);
        }
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new BusinessException(IdentityErrorCode.FRIENDSHIP_NOT_PENDING);
        }
        repository.delete(friendship);
    }

    @Transactional
    public void removeFriend(UUID friendshipId, UUID currentUserId) {
        Friendship friendship = repository.findById(friendshipId)
                .orElseThrow(() -> new BusinessException(IdentityErrorCode.FRIENDSHIP_NOT_FOUND));
        if (!friendship.getRequesterId().equals(currentUserId)
                && !friendship.getReceiverId().equals(currentUserId)) {
            throw new BusinessException(IdentityErrorCode.FRIENDSHIP_FORBIDDEN);
        }
        if (friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new BusinessException(IdentityErrorCode.FRIENDSHIP_NOT_FOUND);
        }
        repository.delete(friendship);
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SliceResponse<FriendshipResponse> getFriends(UUID userId, int page, int size) {
        Slice<FriendshipResponse> slice = repository
                .findAllByUserAndStatus(userId, FriendshipStatus.ACCEPTED, PageRequest.of(page, size))
                .map(this::toResponse);
        return SliceResponse.of(slice);
    }

    @Transactional(readOnly = true)
    public SliceResponse<FriendshipResponse> getIncomingRequests(UUID userId, int page, int size) {
        return SliceResponse.of(repository.findIncomingRequests(userId, PageRequest.of(page, size))
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public SliceResponse<FriendshipResponse> getOutgoingRequests(UUID userId, int page, int size) {
        return SliceResponse.of(repository.findOutgoingRequests(userId, PageRequest.of(page, size))
                .map(this::toResponse));
    }

    // ─── PRIVATE ──────────────────────────────────────────────────────────────

    private Friendship findAndValidateReceiver(UUID friendshipId, UUID currentUserId) {
        Friendship friendship = repository.findById(friendshipId)
                .orElseThrow(() -> new BusinessException(IdentityErrorCode.FRIENDSHIP_NOT_FOUND));
        if (!friendship.getReceiverId().equals(currentUserId)) {
            throw new BusinessException(IdentityErrorCode.FRIENDSHIP_FORBIDDEN);
        }
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new BusinessException(IdentityErrorCode.FRIENDSHIP_NOT_PENDING);
        }
        return friendship;
    }

    private FriendshipResponse toResponse(Friendship f) {
        return new FriendshipResponse(
                f.getId(), f.getRequesterId(), f.getReceiverId(),
                f.getStatus(), f.getCreatedAt());
    }
}
