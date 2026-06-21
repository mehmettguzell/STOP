package com.stop.notification_service.service;

import com.stop.notification_service.dto.NotificationResponse;
import com.stop.notification_service.entity.Notification;
import com.stop.notification_service.entity.NotificationType;
import com.stop.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @InjectMocks
    private NotificationService notificationService;

    private UUID userId;
    private UUID notificationId;
    private Notification notification;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
        notification = Notification.builder()
                .id(notificationId)
                .userId(userId)
                .type(NotificationType.MATCH_STARTED)
                .title("Maç başladı")
                .message("Maçınız başladı.")
                .isRead(false)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void getNotifications_returnsListForUser() {
        when(repository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(notification));

        List<NotificationResponse> result = notificationService.getNotifications(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(notificationId);
        assertThat(result.get(0).type()).isEqualTo(NotificationType.MATCH_STARTED);
    }

    @Test
    void getNotifications_emptyList_returnsEmpty() {
        when(repository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());

        List<NotificationResponse> result = notificationService.getNotifications(userId);

        assertThat(result).isEmpty();
    }

    @Test
    void markAsRead_happyPath_setsReadAndReturnsResponse() {
        when(repository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(repository.save(any())).thenReturn(notification);

        NotificationResponse response = notificationService.markAsRead(notificationId, userId);

        assertThat(response.id()).isEqualTo(notificationId);
        assertThat(notification.isRead()).isTrue();
        verify(repository).save(notification);
    }

    @Test
    void markAsRead_notFound_throwsNotFound() {
        when(repository.findById(notificationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(notificationId, userId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void markAsRead_wrongUser_throwsForbidden() {
        when(repository.findById(notificationId)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(notificationId, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void markAllAsRead_callsRepository() {
        notificationService.markAllAsRead(userId);

        verify(repository).markAllAsRead(userId);
    }

    @Test
    void deleteAllNotifications_deletesAllForUser() {
        when(repository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(notification));

        notificationService.deleteAllNotifications(userId);

        verify(repository).deleteAll(List.of(notification));
    }

    @Test
    void save_callsRepositorySave() {
        when(repository.save(notification)).thenReturn(notification);

        notificationService.save(notification);

        verify(repository).save(notification);
    }

    @Test
    void saveAll_callsRepositorySaveAll() {
        List<Notification> list = List.of(notification);
        when(repository.saveAll(list)).thenReturn(list);

        notificationService.saveAll(list);

        verify(repository).saveAll(list);
    }
}
