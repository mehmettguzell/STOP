package com.stop.communication_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "direct_chats")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter @Setter
public class DirectChat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user1_id", nullable = false)
    private UUID user1Id;

    @Column(name = "user2_id", nullable = false)
    private UUID user2Id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
