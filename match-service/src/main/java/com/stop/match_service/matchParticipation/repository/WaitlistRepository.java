package com.stop.match_service.matchParticipation.repository;

import com.stop.match_service.matchParticipation.entity.WaitlistEntry;
import com.stop.match_service.matchParticipation.entity.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WaitlistRepository extends JpaRepository<WaitlistEntry, UUID> {
    Optional<WaitlistEntry> findFirstByMatchIdAndStatusOrderBySortOrderAsc(UUID matchId, WaitlistStatus status);
    List<WaitlistEntry> findAllByMatchIdAndStatusOrderBySortOrderAsc(UUID matchId, WaitlistStatus status);
    Optional<WaitlistEntry> findByMatchIdAndUserIdAndStatus(UUID matchId, UUID userId, WaitlistStatus status);
    Optional<WaitlistEntry> findTopByMatchIdAndUserIdOrderByCreatedAtDesc(UUID matchId, UUID userId);
    long countByMatchIdAndStatus(UUID matchId, WaitlistStatus status);
    boolean existsByMatchIdAndUserIdAndStatus(UUID matchId, UUID userId, WaitlistStatus status);

    @Query("select coalesce(max(w.sortOrder), 0) from WaitlistEntry w where w.match.id = :matchId and w.status = :status")
    int findMaxSortOrder(@Param("matchId") UUID matchId, @Param("status") WaitlistStatus status);
}
