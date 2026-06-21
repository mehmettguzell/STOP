package com.stop.communication_service.repository;

import com.stop.communication_service.entity.HiddenConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface HiddenConversationRepository extends JpaRepository<HiddenConversation, UUID> {
    @Query("SELECT h.chatId FROM HiddenConversation h WHERE h.userId = :userId")
    Set<UUID> findChatIdsByUserId(UUID userId);
    boolean existsByUserIdAndChatId(UUID userId, UUID chatId);
    void deleteByUserIdAndChatId(UUID userId, UUID chatId);

    // N ayrı DELETE yerine tek sorguda tüm katılımcıları temizler
    @Modifying
    @Query("DELETE FROM HiddenConversation h WHERE h.userId IN :userIds AND h.chatId = :chatId")
    void deleteByUserIdInAndChatId(Collection<UUID> userIds, UUID chatId);
}
