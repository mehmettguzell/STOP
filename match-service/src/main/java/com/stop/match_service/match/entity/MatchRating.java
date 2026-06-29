package com.stop.match_service.match.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Formula;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table (
        name = "match_ratings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_match_rating",
                        columnNames = {"match_id", "rater_id", "rated_id"}
                )
        },
        indexes = {
                @Index(name = "idx_ratings_match", columnList = "match_id"),
                @Index(name = "idx_ratings_rated", columnList = "rated_id"),
        }
)
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@Builder
public class MatchRating {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(name = "rater_id", nullable = false)
    private UUID raterId;

    @Column(name = "rated_id", nullable = false)
    private UUID ratedId;

    @Min(1) @Max(5)
    @Column(nullable = false)
    private Integer skill;

    @Min(1) @Max(5)
    @Column(nullable = false)
    private Integer teamwork;

    @Min(1) @Max(5)
    @Column(nullable = false)
    private Integer sportsmanship;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    @PreUpdate
    private void validateSelfRating() {
        if (raterId != null && raterId.equals(ratedId)) {
            throw new IllegalStateException("A user cannot rate themselves.");
        }
    }
}
