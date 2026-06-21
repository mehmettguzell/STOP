package com.stop.communication_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "match_chat_locks")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter @Setter
public class MatchChatLock {

    @Id
    @Column(name = "match_id")
    private UUID matchId;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;
}
