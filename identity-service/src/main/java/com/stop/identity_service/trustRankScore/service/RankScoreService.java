package com.stop.identity_service.trustRankScore.service;

import com.stop.identity_service.common.error.IdentityErrorCode;
import com.stop.identity_service.common.exception.BusinessException;
import com.stop.identity_service.trustRankScore.dto.response.RankScoreHistoryResponse;
import com.stop.identity_service.trustRankScore.dto.response.RankScoreResponse;
import com.stop.identity_service.trustRankScore.entity.RankScore;
import com.stop.identity_service.trustRankScore.entity.RankScoreEventType;
import com.stop.identity_service.trustRankScore.repository.RankScoreRepository;
import com.stop.identity_service.user.entity.user.User;
import com.stop.identity_service.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankScoreService {

    private static final BigDecimal MIN_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAX_SCORE = BigDecimal.TEN;

    private final RankScoreRepository rankScoreRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "rank:score", key = "#userId")
    public RankScoreResponse getScore(UUID userId) {
        User user = findUser(userId);
        return new RankScoreResponse(userId, user.getRankScore());
    }

    @Transactional(readOnly = true)
    public List<RankScoreHistoryResponse> getHistory(UUID userId) {
        findUser(userId);
        return rankScoreRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(value = {"rank:score", "user:self", "user:public", "user:profile"}, key = "#userId")
    public void applyDelta(UUID userId, BigDecimal delta, RankScoreEventType eventType,
                           UUID sourceId, String sourceType) {
        User user = findUser(userId);

        BigDecimal newScore = clamp(user.getRankScore().add(delta));
        user.setRankScore(newScore);
        userRepository.save(user);

        rankScoreRepository.save(RankScore.builder()
                .userId(userId)
                .eventType(eventType)
                .delta(delta)
                .scoreAfter(newScore)
                .sourceId(sourceId)
                .sourceType(sourceType)
                .build());

        log.info("Rank score updated userId={} eventType={} delta={} scoreAfter={}",
                userId, eventType, delta, newScore);
    }

    private BigDecimal clamp(BigDecimal value) {
        return value.max(MIN_SCORE).min(MAX_SCORE).setScale(1, RoundingMode.HALF_UP);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(IdentityErrorCode.USER_NOT_FOUND));
    }

    private RankScoreHistoryResponse toHistoryResponse(RankScore e) {
        return new RankScoreHistoryResponse(
                e.getId(),
                e.getEventType(),
                e.getDelta(),
                e.getScoreAfter(),
                e.getSourceId(),
                e.getSourceType(),
                e.getCreatedAt()
        );
    }
}
