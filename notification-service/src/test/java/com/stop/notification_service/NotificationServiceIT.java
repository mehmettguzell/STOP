package com.stop.notification_service;

import com.stop.notification_service.entity.Notification;
import com.stop.notification_service.entity.NotificationType;
import com.stop.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationServiceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void savesNotificationAndMarksAllAsReadAgainstRealPostgres() {
        UUID userId = UUID.randomUUID();
        notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(NotificationType.MATCH_STARTED)
                .title("Match started")
                .message("Your match has started")
                .build());

        notificationRepository.markAllAsRead(userId);

        assertThat(notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId))
                .allMatch(Notification::isRead);
    }
}
