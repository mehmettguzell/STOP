package com.stop.identity_service.trustRankScore.repository;

import com.stop.identity_service.trustRankScore.entity.RankScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RankScoreRepository extends JpaRepository<RankScore, UUID> {
    List<RankScore> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
