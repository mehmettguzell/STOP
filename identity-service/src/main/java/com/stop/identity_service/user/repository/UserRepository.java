package com.stop.identity_service.user.repository;

import com.stop.identity_service.user.entity.user.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByDisplayName(String displayName);
    boolean existsByPhoneNumber(String phoneNumber);

    @Query("""
        SELECT u FROM User u
        WHERE u.id <> :userId
          AND ((:email IS NOT NULL AND u.email = :email)
            OR (:displayName IS NOT NULL AND u.displayName = :displayName)
            OR (:phoneNumber IS NOT NULL AND u.phoneNumber = :phoneNumber))
    """)
    List<User> findConflictingFields(
            @Param("userId") UUID userId,
            @Param("email") String email,
            @Param("displayName") String displayName,
            @Param("phoneNumber") String phoneNumber);

    Slice<User> findByDisplayNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String displayName, String email, Pageable pageable);
}
