package com.stop.communication_service.controller;

import com.stop.communication_service.config.jwt.SecurityUtils;
import com.stop.communication_service.dto.DirectChatRequest;
import com.stop.communication_service.dto.DirectChatResponse;
import com.stop.communication_service.service.DirectChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class DirectChatController {

    private final DirectChatService directChatService;

    /**
     * Hedef kullanıcıyla DM kanalı oluşturur veya mevcutu döner.
     * Yanıt: { chatId, otherUserId }
     * chatId, WebSocket topic (/topic/chat/{chatId}) ve history için kullanılır.
     */
    @PostMapping("/direct")
    public ResponseEntity<DirectChatResponse> getOrCreateDirectChat(
            @Valid @RequestBody DirectChatRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        DirectChatResponse response = directChatService.getOrCreate(currentUserId, request.targetUserId());
        return ResponseEntity.ok(response);
    }
}
