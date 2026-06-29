package com.stop.identity_service.trustRankScore.service;

import com.stop.identity_service.common.error.IdentityErrorCode;
import com.stop.identity_service.common.exception.BusinessException;
import com.stop.identity_service.trustRankScore.dto.request.TrustScoreOverrideRequest;
import com.stop.identity_service.trustRankScore.dto.response.TrustScoreHistoryResponse;
import com.stop.identity_service.trustRankScore.dto.response.TrustScoreResponse;
import com.stop.identity_service.trustRankScore.entity.TrustScore;
import com.stop.identity_service.trustRankScore.entity.TrustScoreEventType;
import com.stop.identity_service.trustRankScore.repository.TrustScoreRepository;
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
public class TrustScoreService {

    private static final BigDecimal MIN_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAX_SCORE = BigDecimal.TEN;

    private final TrustScoreRepository trustScoreRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "trust:score", key = "#userId")
    public TrustScoreResponse getScore(UUID userId) {
        User user = findUser(userId);
        return new TrustScoreResponse(userId, user.getTrustScore());
    }

    @Transactional(readOnly = true)
    public List<TrustScoreHistoryResponse> getHistory(UUID userId) {
        findUser(userId);
        return trustScoreRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(value = {"trust:score", "user:self", "user:public", "user:profile"}, key = "#userId")
    public TrustScoreResponse override(UUID userId, TrustScoreOverrideRequest request, UUID adminId) {
        User user = findUser(userId);

        BigDecimal newScore = clamp(user.getTrustScore().add(request.delta()));
        user.setTrustScore(newScore);
        userRepository.save(user);

        trustScoreRepository.save(TrustScore.builder()
                .userId(userId)
                .eventType(TrustScoreEventType.ADMIN_OVERRIDE)
                .delta(request.delta())
                .scoreAfter(newScore)
                .sourceType("ADMIN")
                .changedBy(adminId)
                .build());

        log.info("Trust score overridden for userId={} by adminId={} delta={} newScore={}",
                userId, adminId, request.delta(), newScore);

        return new TrustScoreResponse(userId, newScore);
    }

    @Transactional
    @CacheEvict(value = {"trust:score", "user:self", "user:public", "user:profile"}, key = "#userId")
    public void applyDelta(UUID userId, BigDecimal delta, TrustScoreEventType eventType,
                           UUID sourceId, String sourceType) {
        User user = findUser(userId);

        BigDecimal newScore = clamp(user.getTrustScore().add(delta));
        user.setTrustScore(newScore);
        userRepository.save(user);

        trustScoreRepository.save(TrustScore.builder()
                .userId(userId)
                .eventType(eventType)
                .delta(delta)
                .scoreAfter(newScore)
                .sourceId(sourceId)
                .sourceType(sourceType)
                .build());

        log.info("Trust score updated userId={} eventType={} delta={} scoreAfter={}",
                userId, eventType, delta, newScore);
    }

    private BigDecimal clamp(BigDecimal value) {
        return value.max(MIN_SCORE).min(MAX_SCORE).setScale(1, RoundingMode.HALF_UP);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(IdentityErrorCode.USER_NOT_FOUND));
    }

    private TrustScoreHistoryResponse toHistoryResponse(TrustScore e) {
        return new TrustScoreHistoryResponse(
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
