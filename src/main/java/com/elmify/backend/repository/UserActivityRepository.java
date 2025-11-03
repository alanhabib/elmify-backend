package com.elmify.backend.repository;

import com.elmify.backend.entity.User;
import com.elmify.backend.entity.UserActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

  // Find activities by user
  List<UserActivity> findByUser(User user);

  Page<UserActivity> findByUser(User user, Pageable pageable);

  List<UserActivity> findByUserOrderByCreatedAtDesc(User user);

  Page<UserActivity> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

  // Find activities by user ID
  List<UserActivity> findByUserId(Long userId);

  Page<UserActivity> findByUserId(Long userId, Pageable pageable);

  // Find activities by type
  List<UserActivity> findByActivityType(UserActivity.ActivityType activityType);

  Page<UserActivity> findByActivityType(UserActivity.ActivityType activityType, Pageable pageable);

  // Find activities by user and type
  List<UserActivity> findByUserAndActivityType(User user, UserActivity.ActivityType activityType);

  Page<UserActivity> findByUserAndActivityType(
      User user, UserActivity.ActivityType activityType, Pageable pageable);

  // Find activities within date range
  @Query("SELECT ua FROM UserActivity ua WHERE ua.createdAt BETWEEN :startDate AND :endDate")
  List<UserActivity> findByCreatedAtBetween(
      @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

  @Query(
      "SELECT ua FROM UserActivity ua WHERE ua.createdAt BETWEEN :startDate AND :endDate ORDER BY ua.createdAt DESC")
  Page<UserActivity> findByCreatedAtBetween(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      Pageable pageable);

  // Find user activities within date range
  @Query(
      "SELECT ua FROM UserActivity ua WHERE ua.user = :user AND ua.createdAt BETWEEN :startDate AND :endDate")
  List<UserActivity> findByUserAndCreatedAtBetween(
      @Param("user") User user,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  // Find recent activities
  @Query("SELECT ua FROM UserActivity ua WHERE ua.createdAt >= :since ORDER BY ua.createdAt DESC")
  List<UserActivity> findRecentActivities(@Param("since") LocalDateTime since);

  @Query("SELECT ua FROM UserActivity ua WHERE ua.createdAt >= :since ORDER BY ua.createdAt DESC")
  Page<UserActivity> findRecentActivities(@Param("since") LocalDateTime since, Pageable pageable);

  // Find user's recent activities
  @Query(
      "SELECT ua FROM UserActivity ua WHERE ua.user = :user AND ua.createdAt >= :since ORDER BY ua.createdAt DESC")
  List<UserActivity> findUserRecentActivities(
      @Param("user") User user, @Param("since") LocalDateTime since);

  // Find activities by IP address
  List<UserActivity> findByIpAddress(String ipAddress);

  Page<UserActivity> findByIpAddress(String ipAddress, Pageable pageable);

  // Find activities by session
  List<UserActivity> findBySessionId(String sessionId);

  // Count activities
  @Query("SELECT COUNT(ua) FROM UserActivity ua WHERE ua.user = :user")
  long countByUser(@Param("user") User user);

  @Query("SELECT COUNT(ua) FROM UserActivity ua WHERE ua.activityType = :activityType")
  long countByActivityType(@Param("activityType") UserActivity.ActivityType activityType);

  @Query(
      "SELECT COUNT(ua) FROM UserActivity ua WHERE ua.user = :user AND ua.activityType = :activityType")
  long countByUserAndActivityType(
      @Param("user") User user, @Param("activityType") UserActivity.ActivityType activityType);

  // Activity statistics
  @Query("SELECT ua.activityType, COUNT(ua) FROM UserActivity ua GROUP BY ua.activityType")
  List<Object[]> getActivityStatistics();

  @Query(
      "SELECT ua.activityType, COUNT(ua) FROM UserActivity ua WHERE ua.user = :user GROUP BY ua.activityType")
  List<Object[]> getUserActivityStatistics(@Param("user") User user);

  @Query(
      "SELECT ua.activityType, COUNT(ua) FROM UserActivity ua WHERE ua.createdAt BETWEEN :startDate AND :endDate GROUP BY ua.activityType")
  List<Object[]> getActivityStatisticsBetween(
      @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

  // Daily activity counts
  @Query(
      "SELECT DATE(ua.createdAt), COUNT(ua) FROM UserActivity ua WHERE ua.createdAt BETWEEN :startDate AND :endDate GROUP BY DATE(ua.createdAt) ORDER BY DATE(ua.createdAt)")
  List<Object[]> getDailyActivityCounts(
      @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

  // User daily activity counts
  @Query(
      "SELECT DATE(ua.createdAt), COUNT(ua) FROM UserActivity ua WHERE ua.user = :user AND ua.createdAt BETWEEN :startDate AND :endDate GROUP BY DATE(ua.createdAt) ORDER BY DATE(ua.createdAt)")
  List<Object[]> getUserDailyActivityCounts(
      @Param("user") User user,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  // Most active users
  @Query(
      "SELECT ua.user, COUNT(ua) as activityCount FROM UserActivity ua GROUP BY ua.user ORDER BY activityCount DESC")
  Page<Object[]> findMostActiveUsers(Pageable pageable);

  // Most active users in date range
  @Query(
      "SELECT ua.user, COUNT(ua) as activityCount FROM UserActivity ua WHERE ua.createdAt BETWEEN :startDate AND :endDate GROUP BY ua.user ORDER BY activityCount DESC")
  Page<Object[]> findMostActiveUsersBetween(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      Pageable pageable);

  // Login activities
  @Query(
      "SELECT ua FROM UserActivity ua WHERE ua.activityType = 'LOGIN' ORDER BY ua.createdAt DESC")
  Page<UserActivity> findRecentLogins(Pageable pageable);

  @Query(
      "SELECT ua FROM UserActivity ua WHERE ua.user = :user AND ua.activityType = 'LOGIN' ORDER BY ua.createdAt DESC")
  List<UserActivity> findUserLogins(@Param("user") User user);

  // Delete old activities (for cleanup)
  @Query("DELETE FROM UserActivity ua WHERE ua.createdAt < :cutoffDate")
  void deleteOldActivities(@Param("cutoffDate") LocalDateTime cutoffDate);
}
