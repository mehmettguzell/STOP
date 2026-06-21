package com.stop.match_service.match.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MatchRatingReq(
        @NotNull(message = "Rated User ID cannot be null")
        UUID ratedId,

        @NotNull(message = "Skill score is required")
        @Min(value = 1, message = "Skill score must be at least 1")
        @Max(value = 5, message = "Skill score cannot be more than 5")
        Integer skill,

        @NotNull(message = "Teamwork score is required")
        @Min(value = 1, message = "Teamwork score must be at least 1")
        @Max(value = 5, message = "Teamwork score cannot be more than 5")
        Integer teamwork,

        @NotNull(message = "Sportsmanship score is required")
        @Min(value = 1, message = "Sportsmanship score must be at least 1")
        @Max(value = 5, message = "Sportsmanship score cannot be more than 5")
        Integer sportsmanship
) {
}