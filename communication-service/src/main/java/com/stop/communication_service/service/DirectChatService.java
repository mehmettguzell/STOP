package com.stop.communication_service.service;

import com.stop.communication_service.dto.DirectChatResponse;
import com.stop.communication_service.entity.DirectChat;
import com.stop.communication_service.repository.DirectChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DirectChatService {

    private final DirectChatRepository directChatRepository;


    @Transactional
    public DirectChatResponse getOrCreate(UUID currentUserId, UUID targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("Kendinize mesaj gönderemezsiniz.");
        }

        UUID u1 = currentUserId.compareTo(targetUserId) < 0 ? currentUserId : targetUserId;
        UUID u2 = currentUserId.compareTo(targetUserId) < 0 ? targetUserId : currentUserId;

        DirectChat chat = directChatRepository.findByUser1IdAndUser2Id(u1, u2)
                .orElseGet(() -> directChatRepository.save(
                        DirectChat.builder()
                                .user1Id(u1)
                                .user2Id(u2)
                                .build()
                ));

        return new DirectChatResponse(chat.getId(), targetUserId);
    }
}
