package com.stop.identity_service.trustRankScore.controller;

import com.stop.identity_service.config.jwt.SecurityUtils;
import com.stop.identity_service.trustRankScore.dto.request.TrustScoreOverrideRequest;
import com.stop.identity_service.trustRankScore.dto.response.TrustScoreHistoryResponse;
import com.stop.identity_service.trustRankScore.dto.response.TrustScoreResponse;
import com.stop.identity_service.trustRankScore.service.TrustScoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trust")
@RequiredArgsConstructor
public class TrustScoreController {

    private final TrustScoreService trustScoreService;

    @GetMapping("/users/{userId}")
    public ResponseEntity<TrustScoreResponse> getScore(@PathVariable UUID userId) {
        return ResponseEntity.ok(trustScoreService.getScore(userId));
    }

    @GetMapping("/users/{userId}/history")
    public ResponseEntity<List<TrustScoreHistoryResponse>> getHistory(@PathVariable UUID userId) {
        return ResponseEntity.ok(trustScoreService.getHistory(userId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{userId}")
    public ResponseEntity<TrustScoreResponse> override(
            @PathVariable UUID userId,
            @RequestBody @Valid TrustScoreOverrideRequest request
    ) {
        UUID adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(trustScoreService.override(userId, request, adminId));
    }
}
