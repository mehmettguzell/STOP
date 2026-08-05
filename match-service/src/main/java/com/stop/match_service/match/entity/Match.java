package com.stop.match_service.match.entity;

import com.stop.match_service.matchParticipation.entity.TeamType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "matches",
        indexes = {
                @Index(name = "idx_matches_start_time", columnList = "start_time"),
                @Index(name = "idx_matches_status", columnList = "status"),
                @Index(name = "idx_matches_location", columnList = "location"),
                @Index(name = "idx_matches_visibility", columnList = "visibility")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter @Setter
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 255)
    private String location;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Visibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "organizer_id", nullable = false)
    private UUID organizerId;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "participant_count", nullable = false)
    @Builder.Default
    private Integer participantCount = 0;

    @Column(name = "rating_count", nullable = false)
    @Builder.Default
    private Integer ratingCount = 0;

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    // TODO: bu columnu db ye ekle
    @Column(name = "rating_deadline")
    private LocalDateTime ratingDeadline;

    // Forma rengi kurası: hangi takım beyaz giyecek. null = takımlar henüz atanmadı /
    // forma rengi henüz karara bağlanmadı. Diğer takım (karşı taraf) daima siyahtır.
    @Enumerated(EnumType.STRING)
    @Column(name = "white_team", length = 20)
    private TeamType whiteTeam;


    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}