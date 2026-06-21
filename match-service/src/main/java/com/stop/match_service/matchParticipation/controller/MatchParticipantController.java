package com.stop.match_service.matchParticipation.controller;

import com.stop.match_service.config.jwt.SecurityUtils;
import com.stop.match_service.matchParticipation.dto.request.ParticipantRequestReq;
import com.stop.match_service.matchParticipation.dto.request.RemoveParticipationReq;
import com.stop.match_service.matchParticipation.dto.request.TeamAssignmentReq;
import com.stop.match_service.matchParticipation.dto.response.MatchParticipantResponse;
import com.stop.match_service.matchParticipation.service.MatchParticipantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/participation")
public class MatchParticipantController {

    private final MatchParticipantService service;

    @PostMapping("/leave")
    public ResponseEntity<MatchParticipantResponse> leave(@Valid @RequestBody ParticipantRequestReq request) {
        return ResponseEntity.ok(service.leaveMatch(request, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/remove-player")
    public ResponseEntity<MatchParticipantResponse> removePlayer(@Valid @RequestBody RemoveParticipationReq request) {
        return ResponseEntity.ok(service.removePlayer(request, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/matches/{matchId}")
    public ResponseEntity<List<MatchParticipantResponse>> getMatchParticipants(@PathVariable("matchId") UUID matchId) {
        return ResponseEntity.ok(service.getMatchParticipants(matchId));
    }

    @GetMapping("/matches/{matchId}/participants/ids")
    public ResponseEntity<List<String>> getParticipantIds(@PathVariable("matchId") UUID matchId) {
        return ResponseEntity.ok(
                service.getMatchParticipants(matchId).stream()
                        .map(p -> p.userId().toString())
                        .toList()
        );
    }

    @PutMapping("/matches/{matchId}/teams")
    public ResponseEntity<List<MatchParticipantResponse>> assignTeams(
            @PathVariable UUID matchId,
            @Valid @RequestBody TeamAssignmentReq req) {
        return ResponseEntity.ok(service.assignTeams(matchId, req, SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/matches/{matchId}/lineup")
    public ResponseEntity<List<MatchParticipantResponse>> updateLineup(
            @PathVariable UUID matchId,
            @RequestBody List<MatchParticipantService.LineupEntry> entries) {
        return ResponseEntity.ok(service.updateLineupPositions(matchId, entries, SecurityUtils.getCurrentUserId()));
    }
}
