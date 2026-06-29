package com.stop.communication_service.service;

import com.stop.communication_service.dto.*;
import com.stop.communication_service.entity.*;
import com.stop.communication_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PollService {

    private final PollRepository          pollRepository;
    private final PollOptionRepository    optionRepository;
    private final PollVoteRepository      voteRepository;
    private final ChatMessageRepository   messageRepository;
    private final SimpMessagingTemplate   messaging;

    // ── Anket oluştur ────────────────────────────────────────────────────────

    @Transactional
    public ChatMessageResponse createPoll(UUID chatId, UUID creatorId, CreatePollRequest req) {
        Poll poll = Poll.builder()
                .chatId(chatId)
                .creatorId(creatorId)
                .question(req.question())
                .closesAt(req.closesAt())
                .build();

        List<PollOption> opts = new ArrayList<>();
        List<String> texts = req.options();
        for (int i = 0; i < texts.size(); i++) {
            opts.add(PollOption.builder()
                    .poll(poll)
                    .text(texts.get(i).trim())
                    .position(i)
                    .build());
        }
        poll.setOptions(opts);
        poll = pollRepository.save(poll);

        // Chat mesajı olarak kaydet (type=POLL)
        ChatMessage msg = ChatMessage.builder()
                .matchId(chatId)
                .senderId(creatorId)
                .type("POLL")
                .pollId(poll.getId())
                .build();
        msg = messageRepository.save(msg);

        PollResponse pollData = toPollResponse(poll);
        ChatMessageResponse response = toChatResponse(msg, pollData);
        messaging.convertAndSend("/topic/chat/" + chatId, response);
        return response;
    }

    // ── Oy kullan / değiştir ─────────────────────────────────────────────────

    @Transactional
    public PollResponse vote(UUID pollId, UUID userId, UUID optionId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Poll not found"));

        if (poll.getClosesAt() != null && Instant.now().isAfter(poll.getClosesAt())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Poll is closed");
        }

        boolean validOption = poll.getOptions().stream().anyMatch(o -> o.getId().equals(optionId));
        if (!validOption) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid option");
        }

        Optional<PollVote> existing = voteRepository.findByPollIdAndUserId(pollId, userId);
        if (existing.isPresent()) {
            PollVote vote = existing.get();
            if (vote.getOptionId().equals(optionId)) {
                // Aynı seçeneğe tekrar tıkladı — oyunu geri al
                voteRepository.delete(vote);
            } else {
                // Farklı seçenek — oyu değiştir
                vote.setOptionId(optionId);
            }
        } else {
            voteRepository.save(PollVote.builder()
                    .pollId(pollId)
                    .optionId(optionId)
                    .userId(userId)
                    .build());
        }

        PollResponse updated = toPollResponse(poll);
        broadcastPollUpdate(poll.getChatId(), pollId, updated);
        return updated;
    }

    // ── Anket detayı ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PollResponse getPoll(UUID pollId) {
        return toPollResponse(getPollEntity(pollId));
    }

    @Transactional(readOnly = true)
    public Poll getPollEntity(UUID pollId) {
        return pollRepository.findById(pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Poll not found"));
    }

    // ── Yardımcılar ──────────────────────────────────────────────────────────

    public PollResponse toPollResponse(Poll poll) {
        List<PollVote> allVotes = voteRepository.findAllByPollId(poll.getId());

        Map<UUID, List<UUID>> votersByOption = allVotes.stream()
                .collect(Collectors.groupingBy(
                        PollVote::getOptionId,
                        Collectors.mapping(PollVote::getUserId, Collectors.toList())
                ));

        List<PollOptionResponse> options = poll.getOptions().stream()
                .map(opt -> {
                    List<UUID> voters = votersByOption.getOrDefault(opt.getId(), List.of());
                    return new PollOptionResponse(opt.getId(), opt.getText(), opt.getPosition(), voters.size(), voters);
                })
                .toList();

        return new PollResponse(
                poll.getId(),
                poll.getChatId(),
                poll.getCreatorId(),
                poll.getQuestion(),
                options,
                allVotes.size(),
                poll.getCreatedAt(),
                poll.getClosesAt()
        );
    }

    private void broadcastPollUpdate(UUID chatId, UUID pollId, PollResponse pollData) {
        // Tip: POLL_UPDATE — frontend bu mesajı listeye eklemez, mevcut anketi günceller
        ChatMessageResponse update = new ChatMessageResponse(
                null, chatId, null, null, Instant.now(), false, "POLL_UPDATE", pollId, pollData
        );
        messaging.convertAndSend("/topic/chat/" + chatId, update);
    }

    ChatMessageResponse toChatResponse(ChatMessage msg, PollResponse pollData) {
        return new ChatMessageResponse(
                msg.getId(), msg.getMatchId(), msg.getSenderId(),
                null, msg.getSentAt(), false,
                "POLL", msg.getPollId(), pollData
        );
    }
}
