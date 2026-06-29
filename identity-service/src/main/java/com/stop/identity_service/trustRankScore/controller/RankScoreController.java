package com.stop.identity_service.trustRankScore.controller;

import com.stop.identity_service.trustRankScore.dto.response.RankScoreHistoryResponse;
import com.stop.identity_service.trustRankScore.dto.response.RankScoreResponse;
import com.stop.identity_service.trustRankScore.service.RankScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ranking")
@RequiredArgsConstructor
public class RankScoreController {

    private final RankScoreService rankScoreService;

    @GetMapping("/users/{userId}")
    public ResponseEntity<RankScoreResponse> getScore(@PathVariable UUID userId) {
        return ResponseEntity.ok(rankScoreService.getScore(userId));
    }

    @GetMapping("/users/{userId}/history")
    public ResponseEntity<List<RankScoreHistoryResponse>> getHistory(@PathVariable UUID userId) {
        return ResponseEntity.ok(rankScoreService.getHistory(userId));
    }
}
