package com.elmify.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO for tracking listening activity.
 * Used when the audio player reports listening progress.
 */
public record TrackListeningDto(
    @NotNull(message = "Lecture ID is required")
    Long lectureId,

    @NotNull(message = "Play time is required")
    @Positive(message = "Play time must be positive")
    Integer playTimeSeconds     // Number of seconds played since last track
) {}
