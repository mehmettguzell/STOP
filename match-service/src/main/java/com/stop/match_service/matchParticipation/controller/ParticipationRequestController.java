package com.stop.match_service.matchParticipation.controller;

import com.stop.match_service.config.jwt.SecurityUtils;
import com.stop.match_service.matchParticipation.dto.request.ApproveParticipationReq;
import com.stop.match_service.matchParticipation.dto.request.ParticipantRequestReq;
import com.stop.match_service.matchParticipation.dto.response.ParticipationRequestRes;
import com.stop.match_service.matchParticipation.service.ParticipationRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/participation")
@RequiredArgsConstructor
public class ParticipationRequestController {

    private final ParticipationRequestService service;

    @PostMapping("/requests")
    public ResponseEntity<ParticipationRequestRes> requestParticipation(@RequestBody @Valid ParticipantRequestReq participantRequestReq) {
        return ResponseEntity.ok(service.sendRequest(participantRequestReq, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/requests/approve")
    public ResponseEntity<ParticipationRequestRes> approveParticipation(@RequestBody @Valid ApproveParticipationReq req) {
        return ResponseEntity.ok(service.approveRequest(req, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<ParticipationRequestRes> rejectRequestParticipation(@PathVariable("requestId") UUID requestId) {
        return ResponseEntity.ok(service.rejectRequest(requestId, SecurityUtils.getCurrentUserId()));
    }

    @DeleteMapping("/requests/{requestId}/withdraw")
    public ResponseEntity<Void> withdrawParticipation(@PathVariable("requestId") UUID requestId) {
        service.withdrawRequest(requestId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/requests/match/{matchId}")
    public ResponseEntity<List<ParticipationRequestRes>> getPendingRequests(@PathVariable("matchId") UUID matchId) {
        return ResponseEntity.ok(service.getPendingRequests(matchId, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/requests/my/{matchId}")
    public ResponseEntity<ParticipationRequestRes> getMyRequest(@PathVariable("matchId") UUID matchId) {
        return ResponseEntity.ok(service.getMyRequest(matchId, SecurityUtils.getCurrentUserId()));
    }
}
