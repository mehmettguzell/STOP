package com.stop.match_service;

import com.stop.match_service.match.entity.Match;
import com.stop.match_service.match.entity.Status;
import com.stop.match_service.match.entity.Visibility;
import com.stop.match_service.match.repository.MatchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MatchServiceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private MatchRepository matchRepository;

    @Test
    void savesMatchAndFindsDueRatingsAgainstRealPostgres() {
        Match match = Match.builder()
                .title("IT test match")
                .location("Test Arena")
                .startTime(LocalDateTime.now().minusHours(3))
                .visibility(Visibility.PUBLIC)
                .status(Status.COMPLETED)
                .organizerId(UUID.randomUUID())
                .capacity(10)
                .ratingDeadline(LocalDateTime.now().minusMinutes(1))
                .build();

        Match saved = matchRepository.save(match);

        assertThat(matchRepository.findById(saved.getId())).isPresent();
        assertThat(matchRepository.findAllByStatusAndRatingDeadlineBefore(
                Status.COMPLETED, LocalDateTime.now()))
                .extracting(Match::getId)
                .contains(saved.getId());
    }
}
