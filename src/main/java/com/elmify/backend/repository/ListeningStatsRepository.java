package com.elmify.backend.repository;

import com.elmify.backend.entity.ListeningStats;
import com.elmify.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing ListeningStats entities.
 * Provides queries for tracking daily listening progress, calculating streaks,
 * and generating user statistics.
 */
@Repository
public interface ListeningStatsRepository extends JpaRepository<ListeningStats, Long> {

    /**
     * Find stats for a specific user, lecture, and date.
     * Used for upsert operations when tracking listening activity.
     *
     * @param user The user entity
     * @param lectureId The lecture ID
     * @param date The date to query
     * @return Optional containing stats if found
     */
    Optional<ListeningStats> findByUserAndLectureIdAndDate(
        User user,
        Long lectureId,
        LocalDate date
    );

    /**
     * Find all stats for a specific user on a given date.
     * Used for daily summary calculations.
     *
     * @param user The user entity
     * @param date The date to query
     * @return List of listening stats for that date
     */
    @Query("SELECT ls FROM ListeningStats ls WHERE ls.user = :user AND ls.date = :date")
    List<ListeningStats> findByUserAndDate(
        @Param("user") User user,
        @Param("date") LocalDate date
    );

    /**
     * Find stats for a user within a date range.
     * Used for weekly progress (last 7 days).
     *
     * @param user The user entity
     * @param startDate Start of date range (inclusive)
     * @param endDate End of date range (inclusive)
     * @return List of listening stats ordered by date ascending
     */
    @Query("SELECT ls FROM ListeningStats ls " +
           "WHERE ls.user = :user " +
           "AND ls.date BETWEEN :startDate AND :endDate " +
           "ORDER BY ls.date ASC")
    List<ListeningStats> findByUserAndDateRange(
        @Param("user") User user,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find all dates where user met their daily goal.
     * Used for streak calculation.
     * Returns dates in descending order (most recent first).
     *
     * @param user The user entity
     * @param dailyGoalSeconds The daily goal in seconds
     * @return List of dates where goal was met, ordered by date descending
     */
    @Query("SELECT ls.date FROM ListeningStats ls " +
           "WHERE ls.user = :user " +
           "GROUP BY ls.date " +
           "HAVING SUM(ls.totalPlayTime) >= :dailyGoalSeconds " +
           "ORDER BY ls.date DESC")
    List<LocalDate> findDatesWhereGoalMet(
        @Param("user") User user,
        @Param("dailyGoalSeconds") Integer dailyGoalSeconds
    );

    /**
     * Get total listening time for a user (all time).
     * Used for profile statistics.
     *
     * @param user The user entity
     * @return Total play time in seconds
     */
    @Query("SELECT COALESCE(SUM(ls.totalPlayTime), 0) FROM ListeningStats ls WHERE ls.user = :user")
    Long getTotalListeningTime(@Param("user") User user);

    /**
     * Get count of unique lectures listened to by user.
     * Used for profile statistics.
     *
     * @param user The user entity
     * @return Count of unique lectures
     */
    @Query("SELECT COUNT(DISTINCT ls.lecture.id) FROM ListeningStats ls WHERE ls.user = :user")
    Long getUniqueLecturesCount(@Param("user") User user);
}
