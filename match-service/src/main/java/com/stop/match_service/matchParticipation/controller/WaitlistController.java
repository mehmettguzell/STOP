package com.stop.match_service.matchParticipation.controller;

import com.stop.match_service.config.jwt.SecurityUtils;
import com.stop.match_service.matchParticipation.dto.request.ParticipantRequestReq;
import com.stop.match_service.matchParticipation.dto.request.WaitlistRemoveReq;
import com.stop.match_service.matchParticipation.dto.request.WaitlistReorderReq;
import com.stop.match_service.matchParticipation.dto.response.WaitlistEntryResponse;
import com.stop.match_service.matchParticipation.service.WaitlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/participation")
public class WaitlistController {

    private final WaitlistService service;

    @PostMapping("/waitlist/join")
    public ResponseEntity<WaitlistEntryResponse> join(@Valid @RequestBody ParticipantRequestReq request) {
        return ResponseEntity.ok(service.joinWaitlist(request, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/waitlist/leave")
    public ResponseEntity<Void> leave(@Valid @RequestBody ParticipantRequestReq request) {
        service.leaveWaitlist(request, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/waitlist/remove")
    public ResponseEntity<Void> remove(@Valid @RequestBody WaitlistRemoveReq request) {
        service.removeFromWaitlist(request.matchId(), request.userId(), SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/waitlist/{matchId}/reorder")
    public ResponseEntity<List<WaitlistEntryResponse>> reorder(
            @PathVariable UUID matchId,
            @Valid @RequestBody WaitlistReorderReq request) {
        return ResponseEntity.ok(service.reorderWaitlist(matchId, request.entryIds(), SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/waitlist/{matchId}")
    public ResponseEntity<List<WaitlistEntryResponse>> getWaitlist(@PathVariable UUID matchId) {
        return ResponseEntity.ok(service.getWaitlist(matchId, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/waitlist/my/{matchId}")
    public ResponseEntity<WaitlistEntryResponse> getMyEntry(@PathVariable UUID matchId) {
        return ResponseEntity.ok(service.getMyWaitlistEntry(matchId, SecurityUtils.getCurrentUserId()));
    }
}
