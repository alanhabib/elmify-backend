package com.audibleclone.backend.dto;

import java.time.LocalDate;

/**
 * Daily listening summary for a specific date.
 * Contains today's progress toward the user's daily goal.
 */
public record DailySummaryDto(
    LocalDate date,
    Integer todayMinutes,           // Total minutes listened today
    Integer dailyGoalMinutes,       // User's daily goal in minutes
    Boolean goalMet,                // Whether goal was met
    Integer remainingMinutes        // Minutes remaining to meet goal (0 if met)
) {
    /**
     * Factory method to create a DailySummaryDto from raw data.
     *
     * @param date The date for this summary
     * @param totalSeconds Total seconds listened on this date
     * @param dailyGoalMinutes User's daily goal in minutes
     * @return A new DailySummaryDto instance
     */
    public static DailySummaryDto create(
        LocalDate date,
        Integer totalSeconds,
        Integer dailyGoalMinutes
    ) {
        int todayMinutes = totalSeconds / 60;
        boolean goalMet = todayMinutes >= dailyGoalMinutes;
        int remainingMinutes = Math.max(0, dailyGoalMinutes - todayMinutes);

        return new DailySummaryDto(
            date,
            todayMinutes,
            dailyGoalMinutes,
            goalMet,
            remainingMinutes
        );
    }
}
