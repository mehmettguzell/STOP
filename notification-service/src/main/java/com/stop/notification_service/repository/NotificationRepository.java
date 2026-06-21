package com.stop.notification_service.repository;

import com.stop.notification_service.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.userId = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}
