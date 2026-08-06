package com.stop.identity_service.user.controller;

import com.stop.identity_service.config.jwt.SecurityUtils;
import com.stop.identity_service.userProfile.dto.request.UpdateUserRequest;
import com.stop.identity_service.user.dto.request.BatchUserIdsRequest;
import com.stop.identity_service.user.dto.response.UserPublicResponse;
import com.stop.identity_service.user.dto.response.UserSelfResponse;
import com.stop.identity_service.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.stop.identity_service.userProfile.dto.response.SliceResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserSelfResponse> getMe() {
        return ResponseEntity.ok(userService.getSelfUserResponseById(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/allUsers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SliceResponse<UserSelfResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(SliceResponse.of((search != null && !search.isBlank())
                ? userService.searchUsers(search.trim(), pageRequest)
                : userService.findAllUsers(pageRequest)));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(@PathVariable UUID userId) {
        if ("ADMIN".equals(SecurityUtils.getCurrentUserRole())) {
            return ResponseEntity.ok(userService.getSelfUserResponseById(userId));
        }
        return ResponseEntity.ok(userService.getPublicUserResponseById(userId));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<UserPublicResponse>> getUsersByIds(@RequestBody @Valid BatchUserIdsRequest request) {
        return ResponseEntity.ok(userService.getPublicUsersByIds(request.userIds()));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserSelfResponse> updateUser(@RequestBody @Valid UpdateUserRequest updateUserRequest) {
        return ResponseEntity.ok(userService.updateUser(SecurityUtils.getCurrentUserId(), updateUserRequest));
    }

    @PatchMapping("/{userId}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserSelfResponse> suspend(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.suspendUser(userId));
    }

    @PatchMapping("/{userId}/unsuspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserSelfResponse> unSuspend(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.unSuspendUser(userId));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser() {
        userService.deleteUser(SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }
}
