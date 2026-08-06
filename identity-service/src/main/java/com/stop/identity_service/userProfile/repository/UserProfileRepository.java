package com.stop.identity_service.userProfile.repository;

import com.stop.identity_service.userProfile.entity.profile.UserProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository
        extends JpaRepository<UserProfile, UUID>,
        JpaSpecificationExecutor<UserProfile> {

    @EntityGraph(attributePaths = "user")
    Optional<UserProfile> findByUserId(UUID userId);

    @Query("select p.avatarUrl from UserProfile p where p.user.id = :userId")
    Optional<String> findAvatarUrlByUserId(UUID userId);

    @Query("select p.user.id as userId, p.avatarUrl as avatarUrl from UserProfile p where p.user.id in :userIds")
    List<AvatarUrlProjection> findAvatarUrlsByUserIdIn(List<UUID> userIds);

    interface AvatarUrlProjection {
        UUID getUserId();
        String getAvatarUrl();
    }
}
