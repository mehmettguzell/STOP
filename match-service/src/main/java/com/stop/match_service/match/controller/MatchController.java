package com.stop.match_service.match.controller;

import com.stop.match_service.common.dto.request.PageableRequest;
import com.stop.match_service.config.jwt.SecurityUtils;
import com.stop.match_service.match.dto.request.*;
import com.stop.match_service.match.dto.response.MatchResponse;
import java.util.List;
import com.stop.match_service.match.entity.Status;
import com.stop.match_service.match.entity.Visibility;
import com.stop.match_service.match.service.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/match")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @GetMapping("/my-matches")
    public ResponseEntity<List<MatchResponse>> getMyMatches(
            @RequestParam(required = false) String title,
            @ModelAttribute @Valid PageableRequest pageable
    ) {
        return ResponseEntity.ok(matchService.getMyMatches(SecurityUtils.getCurrentUserId(), title, pageable.page(), pageable.size()));
    }

    @PostMapping
    public ResponseEntity<MatchResponse> createMatch(@Valid @RequestBody CreateMatchRequest createMatchRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(matchService.createMatch(createMatchRequest, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping
    public ResponseEntity<Page<MatchResponse>> searchMatches(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Visibility visibility,
            @RequestParam(defaultValue = "false") boolean excludeJoined,
            @PageableDefault(size = 20, sort = "startTime") Pageable pageable) {
        return ResponseEntity.ok(matchService.searchMatches(title, location, date, status, visibility, excludeJoined, pageable));
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<MatchResponse> getMatch(@PathVariable UUID matchId) {
        return ResponseEntity.ok(matchService.getMatch(matchId));
    }

    @PatchMapping("/{matchId}")
    public ResponseEntity<MatchResponse> updateMatch(@Valid @RequestBody UpdateMatchRequest updateMatchRequest, @PathVariable UUID matchId) {
        return ResponseEntity.ok(matchService.updateMatch(updateMatchRequest, matchId, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("{matchId}/cancel")
    public ResponseEntity<Void> cancelMatch(@PathVariable UUID matchId) {
        matchService.cancelMatch(matchId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{matchId}/transfer-organizer")
    public ResponseEntity<MatchResponse> transferOrganizer(@PathVariable UUID matchId, @Valid @RequestBody TransferOrganizerRequest request) {
        return ResponseEntity.ok(matchService.transferOrganizer(matchId, request.newOrganizerId(), SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/{matchId}/start")
    public ResponseEntity<MatchResponse> startMatch(@PathVariable UUID matchId) {
        return ResponseEntity.ok(matchService.startMatch(matchId, SecurityUtils.getCurrentUserId()));
    }
    
    @PostMapping("/{matchId}/complete")
    public ResponseEntity<MatchResponse> completeMatch(
            @PathVariable UUID matchId,
            @RequestBody @Valid CompleteMatchReq req
    ) {
        return ResponseEntity.ok(matchService.completeMatch(matchId, SecurityUtils.getCurrentUserId(), req));
    }
}
