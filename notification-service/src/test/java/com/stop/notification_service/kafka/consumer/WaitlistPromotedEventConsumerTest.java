package com.stop.notification_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stop.notification_service.entity.Notification;
import com.stop.notification_service.entity.NotificationType;
import com.stop.notification_service.kafka.event.WaitlistPromotedEvent;
import com.stop.notification_service.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WaitlistPromotedEventConsumerTest {

    @Mock
    private NotificationService notificationService;

    private ObjectMapper objectMapper;
    private WaitlistPromotedEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        consumer = new WaitlistPromotedEventConsumer(notificationService, objectMapper);
    }

    @Test
    void consume_validEvent_savesNotificationForPromotedUserNotOrganizer() throws Exception {
        UUID matchId = UUID.randomUUID();
        UUID promotedUserId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();

        WaitlistPromotedEvent event = new WaitlistPromotedEvent(matchId, promotedUserId, organizerId, Instant.now());

        consumer.consume(objectMapper.writeValueAsString(event));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService).save(captor.capture());
        Notification notification = captor.getValue();

        assertThat(notification.getUserId()).isEqualTo(promotedUserId);
        assertThat(notification.getUserId()).isNotEqualTo(organizerId);
        assertThat(notification.getType()).isEqualTo(NotificationType.WAITLIST_PROMOTED);
        assertThat(notification.getReferenceId()).isEqualTo(matchId);
    }

    @Test
    void consume_malformedPayload_isLoggedAndDoesNotThrow() {
        consumer.consume("not valid json");

        verify(notificationService, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
