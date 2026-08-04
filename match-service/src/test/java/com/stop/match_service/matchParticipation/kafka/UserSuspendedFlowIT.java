package com.stop.match_service.matchParticipation.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stop.match_service.match.entity.Match;
import com.stop.match_service.match.entity.Status;
import com.stop.match_service.match.entity.Visibility;
import com.stop.match_service.match.repository.MatchRepository;
import com.stop.match_service.matchParticipation.entity.MatchParticipant;
import com.stop.match_service.matchParticipation.entity.ParticipantStatus;
import com.stop.match_service.matchParticipation.kafka.consumer.UserSuspendedEventConsumer;
import com.stop.match_service.matchParticipation.kafka.event.UserSuspendedEvent;
import com.stop.match_service.matchParticipation.repository.MatchParticipantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the real cross-service contract end-to-end within match-service alone: a raw
 * "identity.user.suspended" Kafka message (as identity-service's producer would emit it) is
 * consumed by {@link UserSuspendedEventConsumer} and removes the suspended user from their
 * upcoming match, against real ephemeral Postgres + Kafka (Testcontainers), never prod infra.
 */
@Testcontainers
@SpringBootTest
class UserSuspendedFlowIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:4.2.0"));

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MatchParticipantRepository matchParticipantRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void suspendedUserIsRemovedFromUpcomingMatchWhenEventIsConsumed() throws Exception {
        UUID userId = UUID.randomUUID();

        Match match = matchRepository.save(Match.builder()
                .title("Flow IT match")
                .location("Test Arena")
                .startTime(LocalDateTime.now().plusDays(1))
                .visibility(Visibility.PUBLIC)
                .status(Status.OPEN)
                .organizerId(UUID.randomUUID())
                .capacity(10)
                .participantCount(1)
                .build());

        MatchParticipant participant = matchParticipantRepository.save(MatchParticipant.builder()
                .match(match)
                .userId(userId)
                .status(ParticipantStatus.JOINED)
                .build());

        UserSuspendedEvent event = new UserSuspendedEvent(userId, Instant.now());
        kafkaTemplate.send("identity.user.suspended", objectMapper.writeValueAsString(event)).get(10, TimeUnit.SECONDS);

        MatchParticipant updated = awaitStatus(participant.getId(), ParticipantStatus.REMOVED);
        assertThat(updated.getStatus()).isEqualTo(ParticipantStatus.REMOVED);
    }

    private MatchParticipant awaitStatus(UUID participantId, ParticipantStatus expected) throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            MatchParticipant current = matchParticipantRepository.findById(participantId).orElseThrow();
            if (current.getStatus() == expected) {
                return current;
            }
            Thread.sleep(500);
        }
        return matchParticipantRepository.findById(participantId).orElseThrow();
    }
}
