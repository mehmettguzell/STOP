package com.stop.identity_service.userProfile.controller;

import com.stop.identity_service.config.jwt.SecurityUtils;
import com.stop.identity_service.userProfile.dto.request.UpdateUserProfileRequest;
import com.stop.identity_service.user.dto.request.UserProfileRequest;
import com.stop.identity_service.userProfile.dto.response.UserProfileResponse;
import com.stop.identity_service.userProfile.entity.profile.UserProfile;
import com.stop.identity_service.userProfile.repository.spec.UserProfileSpec;
import com.stop.identity_service.userProfile.service.UserProfileService;
import com.stop.identity_service.userProfile.service.avatar.AvatarStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.stop.identity_service.common.error.IdentityErrorCode;
import com.stop.identity_service.common.exception.BusinessException;
import com.stop.identity_service.userProfile.dto.response.SliceResponse;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/users/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final AvatarStorageService avatarStorageService;

    @PostMapping
    public ResponseEntity<UserProfileResponse> createUserProfile(@RequestBody @Valid UserProfileRequest userProfileRequest) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(userProfileService.createProfile(userId, userProfileRequest));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable UUID userId) {
        return ResponseEntity.ok(userProfileService.getUserProfile(userId));
    }

    @PatchMapping
    public ResponseEntity<UserProfileResponse> updateUserProfile(@RequestBody @Valid UpdateUserProfileRequest request) {
        return ResponseEntity.ok(userProfileService.updateUserProfile(SecurityUtils.getCurrentUserId(), request));
    }

    @PostMapping("/avatar")
    public ResponseEntity<UserProfileResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        UUID userId = SecurityUtils.getCurrentUserId();
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(IdentityErrorCode.INVALID_IMAGE_FILE);
        }
        return ResponseEntity.ok(userProfileService.updateAvatar(userId, bytes));
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<UserProfileResponse> deleteAvatar() {
        return ResponseEntity.ok(userProfileService.deleteAvatar(SecurityUtils.getCurrentUserId()));
    }

    // Public (no auth) - the filename is an unguessable, server-generated UUID, and profile
    // photos are visible to anyone who can view the profile in the first place. Long-lived
    // immutable caching is safe because a given filename is never overwritten (replace/delete
    // always mint a new filename and remove the old one).
    @GetMapping("/avatar/{filename}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename) {
        Resource resource = avatarStorageService.loadAvatar(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                .body(resource);
    }

    @GetMapping
    public ResponseEntity<SliceResponse<UserProfileResponse>> searchProfiles(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String dominantFoot,
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) Integer minHeight,
            @RequestParam(required = false) Integer maxHeight,
            @RequestParam(required = false) Integer minWeight,
            @RequestParam(required = false) Integer maxWeight,
            @RequestParam(required = false) BigDecimal minTrustScore,
            @RequestParam(required = false) BigDecimal maxTrustScore,
            @RequestParam(required = false) BigDecimal minRankScore,
            @RequestParam(required = false) BigDecimal maxRankScore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Specification<UserProfile> spec = UserProfileSpec.withFetch()
                .and(UserProfileSpec.displayNameLike(displayName))
                .and(UserProfileSpec.cityEq(city))
                .and(UserProfileSpec.positionEq(position))
                .and(UserProfileSpec.dominantFootEq(dominantFoot))
                .and(UserProfileSpec.heightBetween(minHeight, maxHeight))
                .and(UserProfileSpec.weightBetween(minWeight, maxWeight))
                .and(UserProfileSpec.trustScoreBetween(minTrustScore, maxTrustScore))
                .and(UserProfileSpec.rankScoreBetween(minRankScore, maxRankScore));

        return ResponseEntity.ok(SliceResponse.of(userProfileService.searchProfiles(
                spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")))));
    }
}
