package com.stop.communication_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "match_chat_participants")
@IdClass(MatchChatParticipant.PK.class)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter @Setter
public class MatchChatParticipant {

    @Id
    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PK implements Serializable {
        private UUID matchId;
        private UUID userId;
    }
}
