package com.stop.communication_service.controller;

import com.stop.communication_service.config.jwt.SecurityUtils;
import com.stop.communication_service.dto.ChatMessageResponse;
import com.stop.communication_service.dto.CreatePollRequest;
import com.stop.communication_service.dto.PollResponse;
import com.stop.communication_service.dto.VoteRequest;
import com.stop.communication_service.service.PollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;

    @PostMapping("/{chatId}/polls")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse createPoll(
            @PathVariable UUID chatId,
            @Valid @RequestBody CreatePollRequest req
    ) {
        return pollService.createPoll(chatId, SecurityUtils.getCurrentUserId(), req);
    }

    @PostMapping("/polls/{pollId}/vote")
    public PollResponse vote(
            @PathVariable UUID pollId,
            @Valid @RequestBody VoteRequest req
    ) {
        return pollService.vote(pollId, SecurityUtils.getCurrentUserId(), req.optionId());
    }

    @GetMapping("/polls/{pollId}")
    public PollResponse getPoll(@PathVariable UUID pollId) {
        return pollService.getPoll(pollId);
    }
}
