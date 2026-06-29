package com.stop.identity_service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    public static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID USER1_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID USER2_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    public static final UUID USER3_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    private final JdbcTemplate    jdbc;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        String  hash = passwordEncoder.encode("Memozeko123!");
        Instant now  = Instant.now();

        // ── Users (idempotent per email) ───────────────────────────────────────
        insertUserIfAbsent(ADMIN_ID, "memozeko@example.com", "+905550000001", "Memozeko", "ADMIN", hash, now);
        insertUserIfAbsent(USER1_ID, "user@example.com",     "+905550000002", "UserOne",  "USER",  hash, now);
        insertUserIfAbsent(USER2_ID, "user2@example.com",    "+905550000003", "UserTwo",  "USER",  hash, now);
        insertUserIfAbsent(USER3_ID, "user3@example.com",    "+905550000004", "UserThree","USER",  hash, now);

        // ── Profiles ──────────────────────────────────────────────────────────
        insertProfileIfAbsent(ADMIN_ID, "Mehmet", "Yönetici", "İstanbul", "Forvet",    180, 75, "RIGHT", now);
        insertProfileIfAbsent(USER1_ID, "Ali",    "Yıldız",   "Ankara",   "Orta Saha", 175, 70, "RIGHT", now);
        insertProfileIfAbsent(USER2_ID, "Burak",  "Şahin",    "İzmir",    "Kaleci",    185, 82, "LEFT",  now);
        insertProfileIfAbsent(USER3_ID, "Caner",  "Demir",    "Bursa",    "Defans",    178, 73, "RIGHT", now);

        // ── Friendships ───────────────────────────────────────────────────────
        insertFriendshipIfAbsent("60000000-0000-0000-0000-000000000001", ADMIN_ID, USER1_ID, "ACCEPTED",
                now.minus(10, ChronoUnit.DAYS));
        insertFriendshipIfAbsent("60000000-0000-0000-0000-000000000002", ADMIN_ID, USER2_ID, "ACCEPTED",
                now.minus(7,  ChronoUnit.DAYS));
        insertFriendshipIfAbsent("60000000-0000-0000-0000-000000000003", USER1_ID, USER2_ID, "ACCEPTED",
                now.minus(5,  ChronoUnit.DAYS));
        insertFriendshipIfAbsent("60000000-0000-0000-0000-000000000004", ADMIN_ID, USER3_ID, "ACCEPTED",
                now.minus(3,  ChronoUnit.DAYS));

        log.info("Seed data ensured: 4 users, 4 profiles, 4 friendships.");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void insertUserIfAbsent(UUID id, String email, String phone, String displayName,
                                    String role, String hash, Instant now) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);
        if (count != null && count > 0) return;
        jdbc.update("""
                INSERT INTO users
                    (id, email, phone_number, password_hash, display_name,
                     role, status, email_verified, phone_verified,
                     trust_score, rank_score, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', true, true, 7.0, 7.0, ?, ?)
                """,
                id, email, phone, hash, displayName, role,
                java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));
    }

    private void insertProfileIfAbsent(UUID userId, String firstName, String lastName,
                                       String city, String position, int height, int weight,
                                       String foot, Instant now) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_profiles WHERE user_id = ?", Integer.class, userId);
        if (count != null && count > 0) return;
        jdbc.update("""
                INSERT INTO user_profiles
                    (user_id, first_name, last_name, birth_date, city,
                     position, height_cm, weight_kg, dominant_foot, created_at, updated_at)
                VALUES (?, ?, ?, '1995-06-15', ?, ?, ?, ?, ?, ?, ?)
                """,
                userId, firstName, lastName, city, position, height, weight, foot,
                java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));
    }

    private void insertFriendshipIfAbsent(String id, UUID requesterId, UUID receiverId,
                                          String status, Instant createdAt) {
        jdbc.update("""
                INSERT INTO friendships (id, requester_id, receiver_id, status, created_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT DO NOTHING
                """,
                UUID.fromString(id), requesterId, receiverId, status,
                java.sql.Timestamp.from(createdAt));
    }
}
