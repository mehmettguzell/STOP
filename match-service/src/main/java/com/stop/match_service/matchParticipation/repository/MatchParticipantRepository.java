package com.stop.match_service.matchParticipation.repository;

import com.stop.match_service.matchParticipation.entity.MatchParticipant;
import com.stop.match_service.matchParticipation.entity.ParticipantStatus;
import jakarta.persistence.Entity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, UUID> {
    boolean existsByMatchIdAndUserIdAndStatus(UUID matchId, UUID userId, ParticipantStatus status);
    Optional<MatchParticipant> findByMatchIdAndUserId(UUID matchId, UUID userId);

    @Query("""
        SELECT mp.id FROM MatchParticipant mp
        WHERE mp.match.id = :matchId
    """)
    List<UUID> findAllByMatchId(UUID matchId);


    @EntityGraph(attributePaths = "match")
    List<MatchParticipant> findAllByMatchIdAndStatus(UUID matchId, ParticipantStatus status);

    @EntityGraph(attributePaths = "match")
    List<MatchParticipant> findAllByMatchIdInAndStatus(List<UUID> matchIds, ParticipantStatus status);

    @EntityGraph(attributePaths = "match")
    List<MatchParticipant> findAllByUserIdAndStatus(UUID userId, ParticipantStatus status);

    @Query("""
        SELECT mp FROM MatchParticipant mp
        JOIN FETCH mp.match m
        WHERE mp.userId = :userId AND mp.status = :status
        ORDER BY
            CASE WHEN m.startTime >= CURRENT_TIMESTAMP THEN 0 ELSE 1 END ASC,
            CASE WHEN m.startTime >= CURRENT_TIMESTAMP THEN m.startTime END ASC,
            CASE WHEN m.startTime < CURRENT_TIMESTAMP THEN m.startTime END DESC
    """)
    List<MatchParticipant> findMyMatchesSorted(
            @Param("userId") UUID userId,
            @Param("status") ParticipantStatus status
    );

    @Query("""
        SELECT mp FROM MatchParticipant mp
        JOIN FETCH mp.match m
        WHERE mp.userId = :userId AND mp.status = :status
          AND LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%'))
        ORDER BY
            CASE WHEN m.startTime >= CURRENT_TIMESTAMP THEN 0 ELSE 1 END ASC,
            CASE WHEN m.startTime >= CURRENT_TIMESTAMP THEN m.startTime END ASC,
            CASE WHEN m.startTime < CURRENT_TIMESTAMP THEN m.startTime END DESC
    """)
    List<MatchParticipant> findMyMatchesSortedByTitle(
            @Param("userId") UUID userId,
            @Param("status") ParticipantStatus status,
            @Param("title") String title
    );

}
