package com.stop.identity_service.trustRankScore.repository;

import com.stop.identity_service.trustRankScore.entity.TrustScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TrustScoreRepository extends JpaRepository<TrustScore, UUID> {
    List<TrustScore> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
