package com.stop.match_service.matchParticipation.entity;

import com.stop.match_service.match.entity.Match;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "match_participants",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_match_user", columnNames = {"match_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_mp_match", columnList = "match_id"),
                @Index(name = "idx_mp_user", columnList = "user_id"),
                @Index(name = "idx_mp_match_status", columnList = "match_id, status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ParticipantStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "team")
    private TeamType team;

    @Column(name = "position_x")
    private Float positionX;

    @Column(name = "position_y")
    private Float positionY;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }
}