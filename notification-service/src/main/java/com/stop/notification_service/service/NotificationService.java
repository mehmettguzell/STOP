package com.stop.notification_service.service;

import com.stop.notification_service.common.error.CommonErrorCode;
import com.stop.notification_service.dto.NotificationResponse;
import com.stop.notification_service.entity.Notification;
import com.stop.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository repository;

    @Transactional(readOnly = true)
    @Cacheable(value = "notification:list", key = "#userId")
    public List<NotificationResponse> getNotifications(UUID userId) {
        return repository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "notification:list", key = "#userId")
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, CommonErrorCode.RESOURCE_NOT_FOUND.getMessage()));

        if (!notification.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, CommonErrorCode.FORBIDDEN.getMessage());
        }

        notification.setRead(true);
        return toResponse(repository.save(notification));
    }

    @Transactional
    @CacheEvict(value = "notification:list", key = "#userId")
    public void markAllAsRead(UUID userId) {
        repository.markAllAsRead(userId);
        log.debug("All notifications marked as read. userId={}", userId);
    }

    @Transactional
    @CacheEvict(value = "notification:list", key = "#userId")
    public void deleteAllNotifications(UUID userId) {
        repository.deleteAllByUserId(userId);
        log.info("All notifications deleted userId={}", userId);
    }

    /*
     * =========================
     * PACKAGE-LEVEL ACCESS
     * =========================
     */

    @Transactional
    @CacheEvict(value = "notification:list", key = "#notification.userId")
    public void save(Notification notification) {
        repository.save(notification);
        log.debug("Notification saved. userId={} type={}", notification.getUserId(), notification.getType());
    }

    @Transactional
    public void saveAll(List<Notification> notifications) {
        repository.saveAll(notifications);
        log.debug("Notifications saved. count={}", notifications.size());
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getReferenceId(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
