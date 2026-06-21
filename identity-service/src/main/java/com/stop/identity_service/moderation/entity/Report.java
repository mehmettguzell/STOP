package com.stop.identity_service.moderation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "reports",
        indexes = {
                @Index(name = "idx_reports_target",         columnList = "target_user_id"),
                @Index(name = "idx_reports_status_created", columnList = "status, created_at DESC")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter @Setter
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;

    @Column(name = "target_user_id", nullable = false)
    private UUID targetUserId;

    @Column(name = "match_id")
    private UUID matchId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolve_note", columnDefinition = "TEXT")
    private String resolveNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        createdAt = Instant.now();
        status    = ReportStatus.OPEN;
    }
}
