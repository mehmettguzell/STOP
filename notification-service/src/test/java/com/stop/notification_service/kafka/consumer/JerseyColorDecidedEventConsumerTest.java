package com.stop.notification_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stop.notification_service.entity.Notification;
import com.stop.notification_service.entity.NotificationType;
import com.stop.notification_service.kafka.event.JerseyColorDecidedEvent;
import com.stop.notification_service.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JerseyColorDecidedEventConsumerTest {

    @Mock
    private NotificationService notificationService;

    private ObjectMapper objectMapper;
    private JerseyColorDecidedEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        consumer = new JerseyColorDecidedEventConsumer(notificationService, objectMapper);
    }

    @Test
    void consume_validEvent_createsOneNotificationPerRecipientAcrossBothTeams() throws Exception {
        UUID matchId = UUID.randomUUID();
        UUID whiteUser = UUID.randomUUID();
        UUID blackUser1 = UUID.randomUUID();
        UUID blackUser2 = UUID.randomUUID();

        JerseyColorDecidedEvent event = new JerseyColorDecidedEvent(
                matchId, List.of(whiteUser), List.of(blackUser1, blackUser2), Instant.now()
        );

        consumer.consume(objectMapper.writeValueAsString(event));

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationService).saveAll(captor.capture());
        List<Notification> notifications = captor.getValue();

        assertThat(notifications).hasSize(3);
        assertThat(notifications).allMatch(n -> n.getType() == NotificationType.JERSEY_COLOR_DECIDED);
        assertThat(notifications).allMatch(n -> n.getReferenceId().equals(matchId));
        assertThat(notifications).extracting(Notification::getUserId)
                .containsExactlyInAnyOrder(whiteUser, blackUser1, blackUser2);

        Notification whiteNotification = notifications.stream()
                .filter(n -> n.getUserId().equals(whiteUser)).findFirst().orElseThrow();
        assertThat(whiteNotification.getMessage()).containsIgnoringCase("beyaz");

        Notification blackNotification = notifications.stream()
                .filter(n -> n.getUserId().equals(blackUser1)).findFirst().orElseThrow();
        assertThat(blackNotification.getMessage()).containsIgnoringCase("siyah");
    }

    @Test
    void consume_malformedPayload_isLoggedAndDoesNotThrow() {
        consumer.consume("not valid json");

        verify(notificationService, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }
}
