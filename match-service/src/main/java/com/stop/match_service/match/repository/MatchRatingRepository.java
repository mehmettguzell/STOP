package com.stop.match_service.match.repository;

import com.stop.match_service.match.entity.MatchRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MatchRatingRepository extends JpaRepository<MatchRating, UUID> {

    @Query("""
        SELECT mr.ratedId, AVG((mr.skill + mr.teamwork + mr.sportsmanship) / 3.0)
        FROM MatchRating mr
        WHERE mr.match.id = :matchId
        GROUP BY mr.ratedId
    """)
    List<Object[]> findAvgScoresByMatchId(@Param("matchId") UUID matchId);

    @Query("""
        SELECT mr.match.id, mr.ratedId, AVG((mr.skill + mr.teamwork + mr.sportsmanship) / 3.0)
        FROM MatchRating mr
        WHERE mr.match.id IN :matchIds
        GROUP BY mr.match.id, mr.ratedId
    """)
    List<Object[]> findAvgScoresByMatchIds(@Param("matchIds") List<UUID> matchIds);
}
