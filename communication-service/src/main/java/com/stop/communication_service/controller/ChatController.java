package com.stop.communication_service.controller;

import com.stop.communication_service.config.jwt.SecurityUtils;
import com.stop.communication_service.dto.ChatMessageRequest;
import com.stop.communication_service.dto.ChatMessageResponse;
import com.stop.communication_service.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @MessageMapping("/chat/{matchId}")
    public ChatMessageResponse handleMessage(
            @DestinationVariable UUID matchId,
            @Valid ChatMessageRequest request,
            Principal principal
    ) {
        UUID senderId = extractUserId(principal);
        return chatService.saveAndBroadcast(matchId, senderId, request.content(), request.chatType());
    }

    private UUID extractUserId(Principal principal) {
        if (principal instanceof JwtAuthenticationToken token) {
            Jwt jwt = (Jwt) token.getPrincipal();
            return UUID.fromString(jwt.getSubject());
        }
        throw new IllegalStateException("Authenticated principal is not a JWT");
    }
}
