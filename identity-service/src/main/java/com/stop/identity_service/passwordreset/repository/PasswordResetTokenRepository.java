package com.stop.identity_service.passwordreset.repository;

import com.stop.identity_service.passwordreset.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.userId = :userId")
    void deleteAllByUserId(UUID userId);
}
