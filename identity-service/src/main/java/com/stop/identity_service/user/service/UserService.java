package com.stop.identity_service.user.service;

import com.stop.identity_service.common.error.IdentityErrorCode;
import com.stop.identity_service.common.exception.BusinessException;
import com.stop.identity_service.user.kafka.event.UserDeletedEvent;
import com.stop.identity_service.user.kafka.event.UserSuspendedEvent;
import com.stop.identity_service.user.kafka.event.UserUnsuspendedEvent;
import com.stop.identity_service.user.kafka.event.UserUpdatedEvent;
import com.stop.identity_service.userProfile.dto.request.UpdateUserRequest;
import com.stop.identity_service.user.dto.response.UserPublicResponse;
import com.stop.identity_service.user.dto.response.UserSelfResponse;
import com.stop.identity_service.user.entity.user.Status;
import com.stop.identity_service.user.entity.user.User;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import com.stop.identity_service.userProfile.repository.UserProfileRepository;
import com.stop.identity_service.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    private final ApplicationEventPublisher eventPublisher;



    /*
     * =========================
     * READ OPERATIONS
     * =========================
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "user:self" , key = "#userId")
    public UserSelfResponse getSelfUserResponseById(UUID userId) {
        return toSelfResponse(findUserById(userId), findAvatarUrl(userId));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "user:public", key = "#userId")
    public UserPublicResponse getPublicUserResponseById(UUID userId) {
        return toPublicResponse(findUserById(userId), findAvatarUrl(userId));
    }


    @Transactional(readOnly = true)
    public Slice<UserSelfResponse> findAllUsers(Pageable pageable) {
        return mapWithAvatars(userRepository.findAll(pageable));
    }

    @Transactional(readOnly = true)
    public Slice<UserSelfResponse> searchUsers(String query, Pageable pageable) {
        return mapWithAvatars(userRepository
                .findByDisplayNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query, pageable));
    }

    @Transactional(readOnly = true)
    public List<UserPublicResponse> getPublicUsersByIds(List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> avatarUrls = findAvatarUrls(userIds);
        return userRepository.findAllById(userIds).stream()
                .map(user -> toPublicResponse(user, avatarUrls.get(user.getId())))
                .toList();
    }


    /*
     * =========================
     * UPDATE USER (PATCH /me)
     * =========================
     */

    @Transactional
    @CacheEvict(value = {"user:self", "user:public", "user:profile"}, key = "#userId")
    public UserSelfResponse updateUser(UUID userId, UpdateUserRequest request) {

        User user = findUserById(userId);

        ensureUniqueFields(request, user);

        if (!applyPatch(request, user)) {
            return toSelfResponse(user, findAvatarUrl(userId));
        }

        UserSelfResponse response = toSelfResponse(save(user), findAvatarUrl(userId));
        eventPublisher.publishEvent(new UserUpdatedEvent(userId, Instant.now()));
        return response;
    }


    /*
     * =========================
     * STATUS OPERATIONS
     * =========================
     */

    @Transactional
    @CacheEvict(value = {"user:self", "user:public"}, key = "#userId")
    public UserSelfResponse suspendUser(UUID userId) {

        User user = findUserById(userId);

        if (user.getStatus() == Status.SUSPENDED) {
            return toSelfResponse(user, findAvatarUrl(userId));
        }

        user.setStatus(Status.SUSPENDED);

        UserSelfResponse response = toSelfResponse(save(user), findAvatarUrl(userId));
        eventPublisher.publishEvent(new UserSuspendedEvent(userId, Instant.now()));
        return response;
    }


    @Transactional
    @CacheEvict(value = {"user:self", "user:public"}, key = "#userId")
    public UserSelfResponse unSuspendUser(UUID userId) {

        User user = findUserById(userId);

        if (user.getStatus() == Status.ACTIVE) {
            return toSelfResponse(user, findAvatarUrl(userId));
        }

        user.setStatus(Status.ACTIVE);

        UserSelfResponse response = toSelfResponse(save(user), findAvatarUrl(userId));
        eventPublisher.publishEvent(new UserUnsuspendedEvent(userId, Instant.now()));
        return response;
    }


    @Transactional
    @CacheEvict(value = {"user:self", "user:public", "user:profile"}, key = "#userId")
    public void deleteUser(UUID userId) {

        User user = findUserById(userId);

        if (user.getStatus() == Status.DELETED) {
            return;
        }

        userProfileRepository.findById(userId).ifPresent(userProfileRepository::delete);

        user.setStatus(Status.DELETED);
        user.setPhoneNumber("deleted_" + user.getPhoneNumber());
        user.setEmail("deleted_" + user.getEmail());

        save(user);

        eventPublisher.publishEvent(new UserDeletedEvent(userId, Instant.now()));
    }


    /*
     * =========================
     * CORE ENTITY ACCESS
     * =========================
     */

    public User findUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        IdentityErrorCode.USER_NOT_FOUND
                ));
    }


    public Optional<User> findOptionalByEmail(String email) {
        return userRepository.findByEmail(email);
    }


    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }


    public boolean existsByDisplayName(String displayName) {
        return userRepository.existsByDisplayName(displayName);
    }


    public boolean existsByPhoneNumber(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }


    @Transactional
    public User registerUser(User user) {
        return save(user);
    }

    private User save(User user) {
        return userRepository.save(user);
    }


    /*
     * =========================
     * PATCH HELPERS
     * =========================
     */

    private void ensureUniqueFields(UpdateUserRequest req, User user) {
        String newEmail = req.email() != null && !req.email().equals(user.getEmail()) ? req.email() : null;
        String newName = req.displayName() != null && !req.displayName().equals(user.getDisplayName()) ? req.displayName() : null;
        String newPhone = req.phoneNumber() != null && !req.phoneNumber().equals(user.getPhoneNumber()) ? req.phoneNumber() : null;

        if (newEmail == null && newName == null && newPhone == null) return;

        userRepository.findConflictingFields(user.getId(), newEmail, newName, newPhone)
                .forEach(conflict -> {
                    if (newEmail != null && newEmail.equals(conflict.getEmail()))
                        throw new BusinessException(IdentityErrorCode.EMAIL_ALREADY_EXISTS);
                    if (newName != null && newName.equals(conflict.getDisplayName()))
                        throw new BusinessException(IdentityErrorCode.NAME_ALREADY_EXISTS);
                    if (newPhone != null && newPhone.equals(conflict.getPhoneNumber()))
                        throw new BusinessException(IdentityErrorCode.PHONE_ALREADY_EXISTS);
                });
    }


    private boolean applyPatch(UpdateUserRequest req, User user) {

        boolean changed = false;

        changed |= updateEmail(req, user);
        changed |= updateDisplayName(req, user);
        changed |= updatePhone(req, user);
        changed |= updatePassword(req, user);

        return changed;
    }


    private boolean updateEmail(UpdateUserRequest req, User user) {

        if (req.email() == null ||
                req.email().equals(user.getEmail())) {
            return false;
        }

        user.setEmail(req.email());
        user.setEmailVerified(false);

        return true;
    }


    private boolean updateDisplayName(UpdateUserRequest req, User user) {

        if (req.displayName() == null ||
                req.displayName().equals(user.getDisplayName())) {
            return false;
        }

        user.setDisplayName(req.displayName());

        return true;
    }


    private boolean updatePhone(UpdateUserRequest req, User user) {

        if (req.phoneNumber() == null ||
                req.phoneNumber().equals(user.getPhoneNumber())) {
            return false;
        }

        user.setPhoneNumber(req.phoneNumber());
        user.setPhoneVerified(false);

        return true;
    }


    private boolean updatePassword(UpdateUserRequest req, User user) {

        if (req.password() == null) {
            return false;
        }

        if (!req.password().equals(req.rePassword())) {
            throw new BusinessException(
                    IdentityErrorCode.INVALID_CREDENTIALS
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(req.password())
        );

        // Revoke all refresh tokens to force re-authentication on all sessions
        tokenService.revokeAllRefreshTokens(user.getId());

        return true;
    }


    /*
     * =========================
     * DTO MAPPERS
     * =========================
     */

    private UserSelfResponse toSelfResponse(User user, String avatarUrl) {

        return new UserSelfResponse(
                user.getId(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getDisplayName(),
                user.getRole(),
                user.getStatus(),
                user.getTrustScore(),
                user.getRankScore(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getPhoneVerified(),
                user.getEmailVerified(),
                avatarUrl
        );
    }


    private UserPublicResponse toPublicResponse(User user, String avatarUrl) {

        return new UserPublicResponse(
                user.getId(),
                user.getDisplayName(),
                user.getTrustScore(),
                user.getRankScore(),
                avatarUrl
        );
    }


    /*
     * =========================
     * AVATAR LOOKUP HELPERS
     * =========================
     */

    private String findAvatarUrl(UUID userId) {
        return userProfileRepository.findAvatarUrlByUserId(userId).orElse(null);
    }

    private Map<UUID, String> findAvatarUrls(List<UUID> userIds) {
        // Collectors.toMap rejects null values (Map.merge NPEs on them), and a user
        // without an avatar has a null avatarUrl here - filter those out instead;
        // callers already treat a missing map entry the same as a null avatar.
        return userProfileRepository.findAvatarUrlsByUserIdIn(userIds).stream()
                .filter(projection -> projection.getAvatarUrl() != null)
                .collect(Collectors.toMap(
                        UserProfileRepository.AvatarUrlProjection::getUserId,
                        UserProfileRepository.AvatarUrlProjection::getAvatarUrl));
    }

    private Slice<UserSelfResponse> mapWithAvatars(Slice<User> slice) {
        Map<UUID, String> avatarUrls = findAvatarUrls(
                slice.getContent().stream().map(User::getId).toList());
        return slice.map(user -> toSelfResponse(user, avatarUrls.get(user.getId())));
    }

}