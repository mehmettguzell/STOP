package com.stop.identity_service.friendship.controller;

import com.stop.identity_service.common.dto.PageableRequest;
import com.stop.identity_service.config.jwt.SecurityUtils;
import com.stop.identity_service.friendship.dto.request.FriendRequest;
import com.stop.identity_service.friendship.dto.response.FriendshipResponse;
import com.stop.identity_service.friendship.service.FriendshipService;
import com.stop.identity_service.friendship.dto.response.SliceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
@Tag(name = "Friendship", description = "Arkadaşlık işlemleri")
public class FriendshipController {

    private final FriendshipService friendshipService;

    @PostMapping("/requests")
    @Operation(summary = "Arkadaşlık isteği gönder")
    public ResponseEntity<FriendshipResponse> sendFriendRequest(
            @Valid @RequestBody FriendRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                friendshipService.sendFriendshipRequest(
                        SecurityUtils.getCurrentUserId(), request.receiverId()));
    }

    @GetMapping
    @Operation(summary = "Kabul edilmiş arkadaşları listele")
    public ResponseEntity<SliceResponse<FriendshipResponse>> getFriends(
            @ModelAttribute @Valid PageableRequest pageable
    ) {
        return ResponseEntity.ok(
                friendshipService.getFriends(SecurityUtils.getCurrentUserId(), pageable.page(), pageable.size()));
    }

    @GetMapping("/requests/incoming")
    @Operation(summary = "Gelen bekleyen istekleri listele")
    public ResponseEntity<SliceResponse<FriendshipResponse>> getIncomingRequests(
            @ModelAttribute @Valid PageableRequest pageable
    ) {
        return ResponseEntity.ok(
                friendshipService.getIncomingRequests(SecurityUtils.getCurrentUserId(), pageable.page(), pageable.size()));
    }

    @GetMapping("/requests/outgoing")
    @Operation(summary = "Gönderilen bekleyen istekleri listele")
    public ResponseEntity<SliceResponse<FriendshipResponse>> getOutgoingRequests(
            @ModelAttribute @Valid PageableRequest pageable
    ) {
        return ResponseEntity.ok(
                friendshipService.getOutgoingRequests(SecurityUtils.getCurrentUserId(), pageable.page(), pageable.size()));
    }

    @PatchMapping("/requests/{id}/accept")
    @Operation(summary = "Arkadaşlık isteğini kabul et")
    public ResponseEntity<FriendshipResponse> acceptRequest(@PathVariable UUID id) {
        return ResponseEntity.ok(
                friendshipService.acceptFriendshipRequest(id, SecurityUtils.getCurrentUserId()));
    }

    @PatchMapping("/requests/{id}/reject")
    @Operation(summary = "Arkadaşlık isteğini reddet")
    public ResponseEntity<FriendshipResponse> rejectRequest(@PathVariable UUID id) {
        return ResponseEntity.ok(
                friendshipService.rejectFriendshipRequest(id, SecurityUtils.getCurrentUserId()));
    }


    @DeleteMapping("/requests/{id}")
    @Operation(summary = "Gönderilen isteği iptal et")
    public ResponseEntity<Void> cancelRequest(@PathVariable UUID id) {
        friendshipService.cancelFriendshipRequest(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Arkadaşlıktan çıkar")
    public ResponseEntity<Void> removeFriend(@PathVariable UUID id) {
        friendshipService.removeFriend(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }
}
