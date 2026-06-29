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
import java.util.Optional;
import java.util.UUID;

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
        return toSelfResponse(findUserById(userId));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "user:public", key = "#userId")
    public UserPublicResponse getPublicUserResponseById(UUID userId) {
        return toPublicResponse(findUserById(userId));
    }


    @Transactional(readOnly = true)
    public Slice<UserSelfResponse> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toSelfResponse);
    }

    @Transactional(readOnly = true)
    public Slice<UserSelfResponse> searchUsers(String query, Pageable pageable) {
        return userRepository
                .findByDisplayNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query, pageable)
                .map(this::toSelfResponse);
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
            return toSelfResponse(user);
        }

        UserSelfResponse response = toSelfResponse(save(user));
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
            return toSelfResponse(user);
        }

        user.setStatus(Status.SUSPENDED);

        UserSelfResponse response = toSelfResponse(save(user));
        eventPublisher.publishEvent(new UserSuspendedEvent(userId, Instant.now()));
        return response;
    }


    @Transactional
    @CacheEvict(value = {"user:self", "user:public"}, key = "#userId")
    public UserSelfResponse unSuspendUser(UUID userId) {

        User user = findUserById(userId);

        if (user.getStatus() == Status.ACTIVE) {
            return toSelfResponse(user);
        }

        user.setStatus(Status.ACTIVE);

        UserSelfResponse response = toSelfResponse(save(user));
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

    private UserSelfResponse toSelfResponse(User user) {

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
                user.getEmailVerified()
        );
    }


    private UserPublicResponse toPublicResponse(User user) {

        return new UserPublicResponse(
                user.getDisplayName(),
                user.getTrustScore(),
                user.getRankScore()
        );
    }

}