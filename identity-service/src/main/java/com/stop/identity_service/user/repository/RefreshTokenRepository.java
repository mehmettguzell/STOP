package com.stop.identity_service.user.repository;

import com.stop.identity_service.user.entity.auth.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            update RefreshToken rt
            set rt.revoked = true
            where rt.userId = :userId and rt.revoked = false
            """)
    int revokeAllActiveByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("""
            delete from RefreshToken rt
            where rt.userId = :userId
            """)
    void deleteByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("""
            delete from RefreshToken rt
            where rt.revoked = true or rt.expiresAt < :now
            """)
    int deleteExpiredOrRevoked(@Param("now") Instant now);
}
