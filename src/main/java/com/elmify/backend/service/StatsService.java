package com.elmify.backend.service;

import com.elmify.backend.dto.*;
import com.elmify.backend.entity.ListeningStats;
import com.elmify.backend.entity.Lecture;
import com.elmify.backend.entity.User;
import com.elmify.backend.repository.ListeningStatsRepository;
import com.elmify.backend.repository.LectureRepository;
import com.elmify.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing user listening statistics.
 * Handles daily summaries, streak calculations, weekly progress, and activity tracking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {

    private final ListeningStatsRepository listeningStatsRepository;
    private final UserRepository userRepository;
    private final LectureRepository lectureRepository;
    private final ObjectMapper objectMapper;

    private static final int DEFAULT_DAILY_GOAL_MINUTES = 30;

    /**
     * Get daily summary for authenticated user.
     * Returns today's listening progress and goal status.
     *
     * @param clerkId The authenticated user's Clerk ID
     * @return Daily summary with progress information
     */
    @Transactional(readOnly = true)
    public DailySummaryDto getDailySummary(String clerkId) {
        User user = getUserByClerkId(clerkId);
        LocalDate today = LocalDate.now();

        // Get user's daily goal from preferences
        int dailyGoalMinutes = getDailyGoalFromPreferences(user);

        // Sum up all listening stats for today
        List<ListeningStats> todayStats = listeningStatsRepository.findByUserAndDate(user, today);
        int totalSecondsToday = todayStats.stream()
            .mapToInt(ListeningStats::getTotalPlayTime)
            .sum();

        log.debug("Daily summary for user {}: {} seconds today, goal {} minutes",
            clerkId, totalSecondsToday, dailyGoalMinutes);

        return DailySummaryDto.create(today, totalSecondsToday, dailyGoalMinutes);
    }

    /**
     * Get user's current and best streaks.
     * Calculates consecutive days of meeting daily listening goals.
     *
     * @param clerkId The authenticated user's Clerk ID
     * @return Streak information
     */
    @Transactional(readOnly = true)
    public StreakDto getStreaks(String clerkId) {
        User user = getUserByClerkId(clerkId);
        int dailyGoalMinutes = getDailyGoalFromPreferences(user);
        int dailyGoalSeconds = dailyGoalMinutes * 60;

        // Get all dates where user met goal (ordered DESC - most recent first)
        List<LocalDate> goalMetDates = listeningStatsRepository
            .findDatesWhereGoalMet(user, dailyGoalSeconds);

        if (goalMetDates.isEmpty()) {
            log.debug("No streak data found for user {}", clerkId);
            return new StreakDto(0, 0, null);
        }

        // Calculate current streak (working backwards from today)
        int currentStreak = calculateCurrentStreak(goalMetDates);

        // Calculate best streak (longest consecutive sequence)
        int bestStreak = calculateBestStreak(goalMetDates);

        LocalDate lastActive = goalMetDates.get(0); // First element is most recent

        log.debug("Streaks for user {}: current={}, best={}, lastActive={}",
            clerkId, currentStreak, bestStreak, lastActive);

        return new StreakDto(currentStreak, bestStreak, lastActive);
    }

    /**
     * Get weekly progress (last 7 days including today).
     *
     * @param clerkId The authenticated user's Clerk ID
     * @return Weekly progress with day-by-day breakdown
     */
    @Transactional(readOnly = true)
    public WeeklyProgressDto getWeeklyProgress(String clerkId) {
        User user = getUserByClerkId(clerkId);
        int dailyGoalMinutes = getDailyGoalFromPreferences(user);

        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(6); // Last 7 days including today

        List<ListeningStats> weekStats = listeningStatsRepository
            .findByUserAndDateRange(user, weekAgo, today);

        // Group by date and sum play time
        Map<LocalDate, Integer> dailyTotals = weekStats.stream()
            .collect(Collectors.groupingBy(
                ListeningStats::getDate,
                Collectors.summingInt(ListeningStats::getTotalPlayTime)
            ));

        // Create map for all 7 days (fill missing days with 0)
        Map<LocalDate, DayProgressDto> weekMap = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = weekAgo.plusDays(i);
            int totalSeconds = dailyTotals.getOrDefault(date, 0);
            int minutes = totalSeconds / 60;
            boolean goalMet = minutes >= dailyGoalMinutes;

            weekMap.put(date, new DayProgressDto(minutes, goalMet));
        }

        log.debug("Weekly progress for user {}: {} days with data",
            clerkId, dailyTotals.size());

        return new WeeklyProgressDto(weekMap);
    }

    /**
     * Track listening activity (upsert).
     * Updates or creates listening stats for the current date.
     *
     * @param clerkId The authenticated user's Clerk ID
     * @param trackDto Tracking data with lecture ID and play time
     */
    @Transactional
    public void trackListening(String clerkId, TrackListeningDto trackDto) {
        User user = getUserByClerkId(clerkId);
        Lecture lecture = lectureRepository.findById(trackDto.lectureId())
            .orElseThrow(() -> new RuntimeException("Lecture not found: " + trackDto.lectureId()));

        LocalDate today = LocalDate.now();

        // Find existing or create new
        ListeningStats stats = listeningStatsRepository
            .findByUserAndLectureIdAndDate(user, lecture.getId(), today)
            .orElseGet(() -> {
                ListeningStats newStats = new ListeningStats();
                newStats.setUser(user);
                newStats.setLecture(lecture);
                newStats.setDate(today);
                newStats.setTotalPlayTime(0);
                newStats.setPlayCount(0);
                newStats.setCompletionRate(0.0f);
                return newStats;
            });

        // Update stats
        stats.setTotalPlayTime(stats.getTotalPlayTime() + trackDto.playTimeSeconds());
        stats.setPlayCount(stats.getPlayCount() + 1);

        // Calculate completion rate
        if (lecture.getDuration() != null && lecture.getDuration() > 0) {
            float completionRate = (float) stats.getTotalPlayTime() / lecture.getDuration() * 100f;
            stats.setCompletionRate(Math.min(completionRate, 100f));
        }

        listeningStatsRepository.save(stats);

        log.info("Tracked {} seconds for lecture {} for user {} on {}",
            trackDto.playTimeSeconds(), lecture.getId(), clerkId, today);
    }

    // ==================== Helper Methods ====================

    /**
     * Get user by Clerk ID or throw exception.
     */
    private User getUserByClerkId(String clerkId) {
        return userRepository.findByClerkId(clerkId)
            .orElseThrow(() -> new RuntimeException("User not found with clerkId: " + clerkId));
    }

    /**
     * Extract daily goal from user preferences or return default.
     */
    private int getDailyGoalFromPreferences(User user) {
        try {
            if (user.getPreferences() != null && !user.getPreferences().isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> prefs = objectMapper.readValue(
                    user.getPreferences(),
                    Map.class
                );
                Object goal = prefs.get("dailyGoalMinutes");
                if (goal != null) {
                    return ((Number) goal).intValue();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse preferences for user {}, using default", user.getClerkId(), e);
        }
        return DEFAULT_DAILY_GOAL_MINUTES;
    }

    /**
     * Calculate current streak working backwards from today.
     * Streak is active if user met goal today OR yesterday.
     *
     * @param goalMetDates List of dates where goal was met (DESC order)
     * @return Current consecutive days streak
     */
    private int calculateCurrentStreak(List<LocalDate> goalMetDates) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // Streak is broken if user didn't meet goal today or yesterday
        if (!goalMetDates.contains(today) && !goalMetDates.contains(yesterday)) {
            return 0;
        }

        int streak = 0;
        LocalDate currentDate = goalMetDates.contains(today) ? today : yesterday;

        // Count consecutive days backwards
        for (LocalDate date : goalMetDates) {
            if (date.equals(currentDate)) {
                streak++;
                currentDate = currentDate.minusDays(1);
            } else if (date.isBefore(currentDate)) {
                // Gap found, streak broken
                break;
            }
        }

        return streak;
    }

    /**
     * Calculate best streak (longest consecutive sequence ever).
     *
     * @param goalMetDates List of dates where goal was met (DESC order)
     * @return Longest consecutive days streak
     */
    private int calculateBestStreak(List<LocalDate> goalMetDates) {
        if (goalMetDates.isEmpty()) {
            return 0;
        }

        // Ensure sorted in descending order
        List<LocalDate> sortedDates = new ArrayList<>(goalMetDates);
        Collections.sort(sortedDates, Collections.reverseOrder());

        int maxStreak = 1;
        int currentStreakCount = 1;
        LocalDate previousDate = sortedDates.get(0);

        for (int i = 1; i < sortedDates.size(); i++) {
            LocalDate currentDate = sortedDates.get(i);

            // Check if dates are consecutive
            if (previousDate.minusDays(1).equals(currentDate)) {
                currentStreakCount++;
                maxStreak = Math.max(maxStreak, currentStreakCount);
            } else {
                // Streak broken, reset counter
                currentStreakCount = 1;
            }

            previousDate = currentDate;
        }

        return maxStreak;
    }
}
