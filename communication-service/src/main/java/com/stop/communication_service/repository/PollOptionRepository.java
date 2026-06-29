package com.stop.communication_service.repository;

import com.stop.communication_service.entity.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PollOptionRepository extends JpaRepository<PollOption, UUID> {
    List<PollOption> findAllByPollIdOrderByPositionAsc(UUID pollId);
}
