package com.stop.communication_service.repository;

import com.stop.communication_service.entity.DirectChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DirectChatRepository extends JpaRepository<DirectChat, UUID> {

    Optional<DirectChat> findByUser1IdAndUser2Id(UUID user1Id, UUID user2Id);

    @Query("SELECT dc FROM DirectChat dc WHERE dc.user1Id = :userId OR dc.user2Id = :userId")
    List<DirectChat> findAllByUserId(@Param("userId") UUID userId);
}
