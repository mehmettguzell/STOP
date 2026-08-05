package com.stop.identity_service.userProfile.service;

import com.stop.identity_service.common.error.IdentityErrorCode;
import com.stop.identity_service.common.exception.BusinessException;
import com.stop.identity_service.userProfile.dto.request.UpdateUserProfileRequest;
import com.stop.identity_service.user.dto.request.UserProfileRequest;
import com.stop.identity_service.userProfile.dto.response.UserProfileResponse;
import com.stop.identity_service.user.entity.user.User;
import com.stop.identity_service.userProfile.entity.profile.UserProfile;
import com.stop.identity_service.userProfile.repository.UserProfileRepository;
import com.stop.identity_service.userProfile.service.avatar.AvatarStorageService;
import com.stop.identity_service.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserService userService;
    private final UserProfileRepository userProfileRepository;
    private final AvatarStorageService avatarStorageService;

    @Transactional
    @CacheEvict(value = {"user:profile", "user:self", "user:public"}, key = "#userId")
    public UserProfileResponse createProfile(UUID userId, UserProfileRequest request) {
        log.info("Create profile userId={}", userId);
        User user = userService.findUserById(userId);
        ensureProfileNotExists(userId);
        UserProfile saved = userProfileRepository.save(mapToEntity(request, user));
        log.debug("Profile created userId={}", userId);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Slice<UserProfileResponse> searchProfiles(Specification<UserProfile> spec, Pageable pageable) {
        log.debug("Search profiles page={} size={}", pageable.getPageNumber(), pageable.getPageSize());
        return userProfileRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "user:profile", key = "#userId")
    public UserProfileResponse getUserProfile(UUID userId) {
        log.debug("Get profile userId={}", userId);
        return mapToResponse(findUserProfile(userId));
    }

    @Transactional
    @CacheEvict(value = {"user:profile", "user:self", "user:public"}, key = "#userId")
    public UserProfileResponse updateUserProfile(UUID userId, UpdateUserProfileRequest request) {
        log.info("Update profile userId={}", userId);
        UserProfile profile = findUserProfile(userId);
        if (!profile.applyPatch(request)) {
            log.debug("Profile unchanged userId={}", userId);
            return mapToResponse(profile);
        }
        UserProfile saved = userProfileRepository.save(profile);
        log.debug("Profile updated userId={}", userId);
        return mapToResponse(saved);
    }

    @Transactional
    @CacheEvict(value = {"user:profile", "user:self", "user:public"}, key = "#userId")
    public UserProfileResponse updateAvatar(UUID userId, byte[] fileBytes) {
        log.info("Update avatar userId={}", userId);
        UserProfile profile = findUserProfile(userId);
        String previousAvatarUrl = profile.getAvatarUrl();

        String newAvatarUrl = avatarStorageService.uploadAvatar(fileBytes);
        profile.setAvatarUrl(newAvatarUrl);
        UserProfile saved = userProfileRepository.save(profile);

        if (previousAvatarUrl != null) {
            try {
                avatarStorageService.deleteObject(previousAvatarUrl);
            } catch (RuntimeException e) {
                // Orphaned old object is a storage-cost concern, not correctness/security -
                // must not fail the user's successful upload of their new photo.
                log.warn("Failed to delete previous avatar userId={}", userId, e);
            }
        }
        log.debug("Avatar updated userId={}", userId);
        return mapToResponse(saved);
    }

    @Transactional
    @CacheEvict(value = {"user:profile", "user:self", "user:public"}, key = "#userId")
    public UserProfileResponse deleteAvatar(UUID userId) {
        log.info("Delete avatar userId={}", userId);
        UserProfile profile = findUserProfile(userId);
        String currentAvatarUrl = profile.getAvatarUrl();
        if (currentAvatarUrl != null) {
            avatarStorageService.deleteObject(currentAvatarUrl);
            profile.setAvatarUrl(null);
            userProfileRepository.save(profile);
            log.debug("Avatar deleted userId={}", userId);
        }
        return mapToResponse(profile);
    }

    private UserProfile findUserProfile(UUID userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Profile not found userId={}", userId);
                    return new BusinessException(IdentityErrorCode.PROFILE_NOT_FOUND);
                });
    }

    private void ensureProfileNotExists(UUID userId) {
        if (userProfileRepository.existsById(userId)) {
            log.warn("Profile already exists userId={}", userId);
            throw new BusinessException(IdentityErrorCode.PROFILE_ALREADY_EXISTS);
        }
    }

    private UserProfile mapToEntity(UserProfileRequest request, User user) {
        return UserProfile.builder()
                .user(user)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .birthDate(request.birthDate())
                .city(request.city())
                .position(request.position())
                .heightCm(request.heightCm())
                .weightKg(request.weightKg())
                .dominantFoot(request.dominantFoot())
                .bio(request.bio() == null || request.bio().isBlank() ? null : request.bio())
                .build();
    }

    private UserProfileResponse mapToResponse(UserProfile profile) {
        return new UserProfileResponse(
                profile.getUserId(),
                profile.getUser().getDisplayName(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getBirthDate(),
                profile.getCity(),
                profile.getPosition(),
                profile.getHeightCm(),
                profile.getWeightKg(),
                profile.getDominantFoot(),
                profile.getBio(),
                profile.getAvatarUrl(),
                profile.getUser().getTrustScore(),
                profile.getUser().getRankScore()
        );
    }

}
