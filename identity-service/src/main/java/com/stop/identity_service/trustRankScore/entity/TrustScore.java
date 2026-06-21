package com.stop.identity_service.trustRankScore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trust_score_events", indexes = {
        @Index(name = "idx_trust_user_time", columnList = "user_id, created_at DESC")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class TrustScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private TrustScoreEventType eventType;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal delta;

    @Column(name = "score_after", nullable = false, precision = 3, scale = 1)
    private BigDecimal scoreAfter;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "source_type", length = 50)
    private String sourceType;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
