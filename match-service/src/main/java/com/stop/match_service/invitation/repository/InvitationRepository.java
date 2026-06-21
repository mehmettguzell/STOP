package com.stop.match_service.invitation.repository;

import com.stop.match_service.invitation.entity.Invitation;
import com.stop.match_service.invitation.entity.InvitationStatus;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    boolean existsByMatchIdAndReceiverIdAndStatus(UUID matchId, UUID receiverId, InvitationStatus status);

    Optional<Invitation> findByReceiverIdAndMatchId(UUID receiverId,UUID matchId);

    Slice<Invitation> findAllByReceiverIdAndStatus(UUID receiverID,
                                                   InvitationStatus status,
                                                   Pageable pageable);
}
