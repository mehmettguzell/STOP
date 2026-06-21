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
@Table(name = "chat_reads")
@IdClass(ChatRead.PK.class)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter @Setter
public class ChatRead {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(name = "chat_id", nullable = false)
    private UUID chatId;

    @Column(name = "last_read_at", nullable = false)
    private Instant lastReadAt;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PK implements Serializable {
        private UUID userId;
        private UUID chatId;
    }
}
