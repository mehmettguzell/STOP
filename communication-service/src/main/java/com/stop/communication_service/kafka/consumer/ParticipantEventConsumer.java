package com.stop.communication_service.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.stop.communication_service.entity.MatchChatLock;
import com.stop.communication_service.entity.MatchChatParticipant;
import com.stop.communication_service.repository.MatchChatLockRepository;
import com.stop.communication_service.repository.MatchChatParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * match.participant.* Kafka olaylarını dinleyerek match_chat_participants tablosunu güncel tutar.
 * Bu tablo; hangi kullanıcının hangi maçın sohbetine erişebileceğini belirler.
 *
 * Her iki event türü de JsonNode ile okunur → farklı alan adları sorun yaratmaz.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ParticipantEventConsumer {

    private final MatchChatParticipantRepository participantRepository;
    private final MatchChatLockRepository matchChatLockRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // ── Katılma olayı ───────────────────────────────────────────────────────
    @KafkaListener(topics = "match.participant.joined", groupId = "communication-service")
    @Transactional
    public void onParticipantJoined(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            UUID matchId = UUID.fromString(node.get("matchId").asText());
            UUID userId  = UUID.fromString(node.get("userId").asText());

            addIfAbsent(matchId, userId);

            // Organizatörü de ekle (katılım eventi organizatörü içerebilir)
            JsonNode orgNode = node.get("organizerId");
            if (orgNode != null && !orgNode.isNull()) {
                addIfAbsent(matchId, UUID.fromString(orgNode.asText()));
            }

        } catch (Exception e) {
            log.error("[ParticipantEventConsumer] joined parse error: {}", payload, e);
        }
    }

    // ── Kendi isteğiyle ayrılma olayı ───────────────────────────────────────
    @KafkaListener(topics = "match.participant.left", groupId = "communication-service")
    @Transactional
    public void onParticipantLeft(String payload) {
        removeParticipant(payload, "left");
    }

    // ── Organizatör tarafından çıkarılma olayı ──────────────────────────────
    @KafkaListener(topics = "match.participant.removed", groupId = "communication-service")
    @Transactional
    public void onParticipantRemoved(String payload) {
        removeParticipant(payload, "removed");
    }

    // ── Maç tamamlandı → chat kilitle ───────────────────────────────────────
    @KafkaListener(topics = "match.match.completed", groupId = "communication-service")
    @Transactional
    public void onMatchCompleted(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            UUID matchId = UUID.fromString(node.get("matchId").asText());
            Instant completedAt = Instant.parse(node.get("completedAt").asText());
            matchChatLockRepository.save(MatchChatLock.builder()
                    .matchId(matchId)
                    .completedAt(completedAt)
                    .build());
            log.debug("[Chat] Match chat locked matchId={}", matchId);
        } catch (Exception e) {
            log.error("[ParticipantEventConsumer] match.completed parse error: {}", payload, e);
        }
    }

    // ── Yardımcı metodlar ───────────────────────────────────────────────────

    private void addIfAbsent(UUID matchId, UUID userId) {
        if (!participantRepository.existsByMatchIdAndUserId(matchId, userId)) {
            participantRepository.save(
                    MatchChatParticipant.builder()
                            .matchId(matchId)
                            .userId(userId)
                            .joinedAt(Instant.now())
                            .build()
            );
            log.debug("[Chat] Participant added matchId={} userId={}", matchId, userId);
        }
    }

    private void removeParticipant(String payload, String eventType) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            UUID matchId = UUID.fromString(node.get("matchId").asText());
            UUID userId  = UUID.fromString(node.get("userId").asText());
            participantRepository.deleteByMatchIdAndUserId(matchId, userId);
            log.debug("[Chat] Participant {} matchId={} userId={}", eventType, matchId, userId);
        } catch (Exception e) {
            log.error("[ParticipantEventConsumer] {} parse error: {}", eventType, payload, e);
        }
    }
}
