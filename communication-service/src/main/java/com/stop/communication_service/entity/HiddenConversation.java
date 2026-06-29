package com.stop.communication_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
    name = "hidden_conversations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "chat_id"})
)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter @Setter
public class HiddenConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "chat_id", nullable = false)
    private UUID chatId;
}
