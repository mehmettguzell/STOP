package com.stop.identity_service.userProfile.repository;

import com.stop.identity_service.userProfile.entity.profile.UserProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository
        extends JpaRepository<UserProfile, UUID>,
        JpaSpecificationExecutor<UserProfile> {

    @EntityGraph(attributePaths = "user")
    Optional<UserProfile> findByUserId(UUID userId);
}
