package com.stop.communication_service.repository;

import com.stop.communication_service.entity.MatchChatLock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MatchChatLockRepository extends JpaRepository<MatchChatLock, UUID> {
}
