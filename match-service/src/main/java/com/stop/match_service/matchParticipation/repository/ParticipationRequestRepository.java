package com.stop.match_service.matchParticipation.repository;

import com.stop.match_service.matchParticipation.entity.ParticipationRequestEntity;
import com.stop.match_service.matchParticipation.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequestEntity, UUID> {
    boolean existsByMatchIdAndUserIdAndStatus(UUID matchId, UUID userId, RequestStatus status);
    Optional<ParticipationRequestEntity> findByIdAndUserId(UUID id, UUID userId);
    List<ParticipationRequestEntity> findAllByMatchIdAndStatus(UUID matchId, RequestStatus status);
    List<ParticipationRequestEntity> findAllByUserIdAndStatus(UUID userId, RequestStatus status);
    Optional<ParticipationRequestEntity> findTopByMatchIdAndUserIdOrderByCreatedAtDesc(UUID matchId, UUID userId);
    long countByMatchIdAndUserIdAndStatus(UUID matchId, UUID userId, RequestStatus status);
}
