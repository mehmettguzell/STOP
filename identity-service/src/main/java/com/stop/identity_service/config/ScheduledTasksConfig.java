package com.stop.identity_service.config;

import com.stop.identity_service.user.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasksConfig {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredRefreshTokens() {
        int deleted = refreshTokenRepository.deleteExpiredOrRevoked(Instant.now());
        if (deleted > 0) {
            log.info("Purged {} expired/revoked refresh tokens", deleted);
        }
    }
}
