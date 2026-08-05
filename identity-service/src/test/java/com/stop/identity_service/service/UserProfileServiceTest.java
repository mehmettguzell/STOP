package com.stop.identity_service.service;

import com.stop.identity_service.user.entity.user.User;
import com.stop.identity_service.userProfile.entity.profile.UserProfile;
import com.stop.identity_service.userProfile.repository.UserProfileRepository;
import com.stop.identity_service.userProfile.service.UserProfileService;
import com.stop.identity_service.userProfile.service.avatar.AvatarStorageService;
import com.stop.identity_service.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private AvatarStorageService avatarStorageService;

    @InjectMocks
    private UserProfileService userProfileService;

    private UserProfile profileWithAvatar(UUID userId, String avatarUrl) {
        User user = User.builder()
                .id(userId)
                .displayName("Player")
                .trustScore(BigDecimal.valueOf(7))
                .rankScore(BigDecimal.valueOf(7))
                .build();
        return UserProfile.builder()
                .userId(userId)
                .user(user)
                .avatarUrl(avatarUrl)
                .build();
    }

    @Test
    void updateAvatar_noExistingPhoto_doesNotDeleteAnything() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = profileWithAvatar(userId, null);
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(avatarStorageService.uploadAvatar(any())).thenReturn("https://bucket.s3.eu-central-1.amazonaws.com/avatars/new.jpg");
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userProfileService.updateAvatar(userId, new byte[]{1, 2, 3});

        verify(avatarStorageService, never()).deleteObject(any());
    }

    @Test
    void updateAvatar_existingPhoto_deletesPreviousObject() {
        UUID userId = UUID.randomUUID();
        String oldUrl = "https://bucket.s3.eu-central-1.amazonaws.com/avatars/old.jpg";
        UserProfile profile = profileWithAvatar(userId, oldUrl);
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(avatarStorageService.uploadAvatar(any())).thenReturn("https://bucket.s3.eu-central-1.amazonaws.com/avatars/new.jpg");
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userProfileService.updateAvatar(userId, new byte[]{1, 2, 3});

        verify(avatarStorageService).deleteObject(eq(oldUrl));
    }

    @Test
    void updateAvatar_oldObjectDeleteFails_doesNotFailTheUpload() {
        UUID userId = UUID.randomUUID();
        String oldUrl = "https://bucket.s3.eu-central-1.amazonaws.com/avatars/old.jpg";
        UserProfile profile = profileWithAvatar(userId, oldUrl);
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(avatarStorageService.uploadAvatar(any())).thenReturn("https://bucket.s3.eu-central-1.amazonaws.com/avatars/new.jpg");
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        org.mockito.Mockito.doThrow(new RuntimeException("s3 unavailable"))
                .when(avatarStorageService).deleteObject(eq(oldUrl));

        userProfileService.updateAvatar(userId, new byte[]{1, 2, 3});
        // No exception propagated - the new upload succeeded and must not be rolled back
        // just because cleaning up the old object failed.
    }

    @Test
    void deleteAvatar_existingPhoto_deletesObjectAndClearsUrl() {
        UUID userId = UUID.randomUUID();
        String url = "https://bucket.s3.eu-central-1.amazonaws.com/avatars/current.jpg";
        UserProfile profile = profileWithAvatar(userId, url);
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = userProfileService.deleteAvatar(userId);

        verify(avatarStorageService).deleteObject(eq(url));
        assertNull(response.avatarUrl());
    }

    @Test
    void deleteAvatar_noPhoto_isNoOpAndDoesNotCallDelete() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = profileWithAvatar(userId, null);
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        var response = userProfileService.deleteAvatar(userId);

        verify(avatarStorageService, never()).deleteObject(any());
        verify(userProfileRepository, never()).save(any());
        assertNull(response.avatarUrl());
    }
}
