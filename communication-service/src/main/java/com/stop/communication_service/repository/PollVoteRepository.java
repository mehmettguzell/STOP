package com.stop.communication_service.repository;

import com.stop.communication_service.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PollVoteRepository extends JpaRepository<PollVote, UUID> {

    Optional<PollVote> findByPollIdAndUserId(UUID pollId, UUID userId);

    List<PollVote> findAllByPollId(UUID pollId);

    @Query("SELECT v.userId FROM PollVote v WHERE v.optionId = :optionId")
    List<UUID> findVoterIdsByOptionId(@Param("optionId") UUID optionId);
}
