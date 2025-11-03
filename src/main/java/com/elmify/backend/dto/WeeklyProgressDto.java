package com.elmify.backend.dto;

import java.time.LocalDate;
import java.util.Map;

/**
 * Weekly progress summary for the last 7 days.
 * Maps dates to their progress information.
 */
public record WeeklyProgressDto(
    Map<LocalDate, DayProgressDto> days
) {}
