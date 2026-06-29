package com.stop.communication_service.repository;

import com.stop.communication_service.entity.MatchChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MatchChatParticipantRepository
        extends JpaRepository<MatchChatParticipant, MatchChatParticipant.PK> {

    List<MatchChatParticipant> findAllByUserId(UUID userId);

    @org.springframework.data.jpa.repository.Query("SELECT p.userId FROM MatchChatParticipant p WHERE p.matchId = :matchId")
    List<UUID> findUserIdsByMatchId(UUID matchId);

    void deleteByMatchIdAndUserId(UUID matchId, UUID userId);

    boolean existsByMatchIdAndUserId(UUID matchId, UUID userId);
}
