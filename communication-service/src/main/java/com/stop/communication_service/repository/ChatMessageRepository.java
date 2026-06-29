package com.stop.communication_service.repository;

import com.stop.communication_service.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findAllByMatchIdOrderBySentAtAsc(UUID matchId);

    // Cursor-based pagination — iki ayrı sorgu: PostgreSQL null parametre tipini çözemez
    @Query("""
            SELECT m FROM ChatMessage m
            WHERE m.matchId = :chatId
            ORDER BY m.sentAt DESC
            """)
    List<ChatMessage> findLatest(
            @Param("chatId") UUID chatId,
            Pageable pageable
    );

    @Query("""
            SELECT m FROM ChatMessage m
            WHERE m.matchId = :chatId
              AND m.sentAt < :before
            ORDER BY m.sentAt DESC
            """)
    List<ChatMessage> findBefore(
            @Param("chatId") UUID chatId,
            @Param("before") Instant before,
            Pageable pageable
    );

    @Query("""
            SELECT m FROM ChatMessage m
            WHERE m.matchId = :matchId
              AND m.deletedAt IS NULL
            ORDER BY m.sentAt DESC
            """)
    List<ChatMessage> findLastMessage(@Param("matchId") UUID matchId, Pageable pageable);


    @Query("""
            SELECT COUNT(m) FROM ChatMessage m
            WHERE m.matchId = :chatId
              AND m.sentAt  > :since
              AND m.senderId <> :userId
              AND m.deletedAt IS NULL
            """)
    long countUnread(
            @Param("chatId")  UUID chatId,
            @Param("since")   Instant since,
            @Param("userId")  UUID userId
    );
}
