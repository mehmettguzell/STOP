package com.stop.communication_service.repository;

import com.stop.communication_service.entity.ChatRead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChatReadRepository extends JpaRepository<ChatRead, ChatRead.PK> {

    Optional<ChatRead> findByUserIdAndChatId(UUID userId, UUID chatId);
}
