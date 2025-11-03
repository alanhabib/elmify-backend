package com.elmify.backend.repository;

import com.elmify.backend.entity.PlaybackPosition;
import com.elmify.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaybackPositionRepository extends JpaRepository<PlaybackPosition, Long> {

    /**
     * Find playback position for a specific user and lecture
     */
    Optional<PlaybackPosition> findByUserAndLectureId(User user, Long lectureId);

    /**
     * Get all playback positions for a user with lecture details (eager loading)
     */
    @Query("SELECT p FROM PlaybackPosition p " +
           "LEFT JOIN FETCH p.lecture l " +
           "LEFT JOIN FETCH l.speaker " +
           "LEFT JOIN FETCH l.collection " +
           "WHERE p.user = :user " +
           "ORDER BY p.lastUpdated DESC")
    List<PlaybackPosition> findByUserWithLecture(@Param("user") User user);

    /**
     * Get recently played lectures (continue listening)
     * Only returns positions where user hasn't finished (currentPosition > 0 and < duration)
     */
    @Query("SELECT p FROM PlaybackPosition p " +
           "LEFT JOIN FETCH p.lecture l " +
           "LEFT JOIN FETCH l.speaker " +
           "LEFT JOIN FETCH l.collection " +
           "WHERE p.user = :user " +
           "AND p.currentPosition > 0 " +
           "AND p.currentPosition < l.duration " +
           "ORDER BY p.lastUpdated DESC")
    List<PlaybackPosition> findContinueListening(@Param("user") User user);

    /**
     * Delete playback position for a user and lecture
     */
    void deleteByUserAndLectureId(User user, Long lectureId);

    /**
     * Check if playback position exists
     */
    boolean existsByUserAndLectureId(User user, Long lectureId);
}
