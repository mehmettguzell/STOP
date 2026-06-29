package com.stop.communication_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "poll_votes",
    uniqueConstraints = @UniqueConstraint(name = "uq_poll_user", columnNames = {"poll_id", "user_id"}),
    indexes = {
        @Index(name = "idx_poll_votes_poll",   columnList = "poll_id"),
        @Index(name = "idx_poll_votes_option", columnList = "option_id")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PollVote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "poll_id", nullable = false)
    private UUID pollId;

    @Column(name = "option_id", nullable = false)
    private UUID optionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @CreationTimestamp
    @Column(name = "voted_at", nullable = false, updatable = false)
    private Instant votedAt;
}
