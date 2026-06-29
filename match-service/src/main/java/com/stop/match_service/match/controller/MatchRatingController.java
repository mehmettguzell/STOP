package com.stop.match_service.match.controller;

import com.stop.match_service.config.jwt.SecurityUtils;
import com.stop.match_service.match.dto.request.MatchRatingReq;
import com.stop.match_service.match.service.MatchRatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/match")
@RequiredArgsConstructor

public class MatchRatingController {
    private final MatchRatingService service;

    @PostMapping("/{matchId}/ratings")
    public ResponseEntity<Object> submitRating(
            @PathVariable("matchId") UUID matchId,
            @RequestBody @Valid MatchRatingReq request
        ){
        service.submitRating(SecurityUtils.getCurrentUserId(), matchId, request);
        return  ResponseEntity.ok().build();
    }
}
