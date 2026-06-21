package com.stop.match_service.invitation.controller;

import com.stop.match_service.common.dto.request.PageableRequest;
import com.stop.match_service.common.dto.response.SliceResponse;
import com.stop.match_service.config.jwt.SecurityUtils;
import com.stop.match_service.invitation.dto.request.InvitationRequest;
import com.stop.match_service.invitation.dto.request.UpdateInvitationRequest;
import com.stop.match_service.invitation.dto.response.InvitationResponse;
import com.stop.match_service.invitation.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping
    public ResponseEntity<InvitationResponse> createInvitation(@RequestBody @Valid InvitationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invitationService.createInvitation(request, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/accept")
    public ResponseEntity<InvitationResponse> acceptInvitation(@RequestBody @Valid UpdateInvitationRequest request) {
        return ResponseEntity.ok(invitationService.acceptInvitation(request, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/reject")
    public ResponseEntity<InvitationResponse> rejectInvitation(@RequestBody @Valid UpdateInvitationRequest request) {
        return ResponseEntity.ok(invitationService.rejectInvitation(request, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping
    public ResponseEntity<SliceResponse<InvitationResponse>> getAllInvitations(
            @ModelAttribute @Valid PageableRequest pageable
    ) {
        return ResponseEntity.ok(invitationService.getAllInvitations(SecurityUtils.getCurrentUserId(), pageable.page(), pageable.size()));
    }
}
