package com.elmify.backend.dto;

import java.time.LocalDate;

/**
 * User's streak information.
 * Tracks consecutive days of meeting daily listening goals.
 */
public record StreakDto(
    Integer currentStreak,          // Current consecutive days of meeting goal
    Integer bestStreak,             // Longest streak ever achieved
    LocalDate lastActiveDate        // Last date user met their daily goal
) {}
