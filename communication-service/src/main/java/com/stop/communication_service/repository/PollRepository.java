package com.stop.communication_service.repository;

import com.stop.communication_service.entity.Poll;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PollRepository extends JpaRepository<Poll, UUID> {}
