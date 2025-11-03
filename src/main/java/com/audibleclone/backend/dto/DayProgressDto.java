package com.audibleclone.backend.dto;

/**
 * Progress information for a single day.
 */
public record DayProgressDto(
    Integer minutes,        // Total minutes listened on this day
    Boolean goalMet        // Whether daily goal was met
) {}
