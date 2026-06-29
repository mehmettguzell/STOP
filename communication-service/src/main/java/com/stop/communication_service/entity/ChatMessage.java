package com.stop.communication_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "chat_messages",
        indexes = {
                @Index(name = "idx_chat_match_id", columnList = "match_id, sent_at DESC")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter @Setter
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(length = 50, nullable = false)
    @Builder.Default
    private String type = "TEXT";

    @Column(length = 1000)
    private String content;

    @Column(name = "poll_id")
    private UUID pollId;

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
