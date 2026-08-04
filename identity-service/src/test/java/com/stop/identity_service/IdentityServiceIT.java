package com.stop.identity_service;

import com.stop.identity_service.user.entity.user.User;
import com.stop.identity_service.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IdentityServiceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesAndFindsUserByEmailAgainstRealPostgres() {
        User user = User.builder()
                .email("it-test@example.com")
                .phoneNumber("+10000000000")
                .passwordHash("hashed")
                .displayName("it-test-user")
                .build();

        userRepository.save(user);

        assertThat(userRepository.findByEmail("it-test@example.com")).isPresent();
        assertThat(userRepository.existsByEmail("it-test@example.com")).isTrue();
        assertThat(userRepository.existsByDisplayName("it-test-user")).isTrue();
    }
}
