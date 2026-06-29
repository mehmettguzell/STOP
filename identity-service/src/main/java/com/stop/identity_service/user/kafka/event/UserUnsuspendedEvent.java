package com.stop.identity_service.user.kafka.event;

import java.time.Instant;
import java.util.UUID;

public record UserUnsuspendedEvent(UUID userId, Instant timestamp) {}
