package com.stop.communication_service.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VoteRequest(@NotNull UUID optionId) {}
