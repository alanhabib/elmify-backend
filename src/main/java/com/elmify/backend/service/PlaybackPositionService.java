package com.elmify.backend.service;

import com.elmify.backend.entity.Lecture;
import com.elmify.backend.entity.PlaybackPosition;
import com.elmify.backend.entity.User;
import com.elmify.backend.repository.LectureRepository;
import com.elmify.backend.repository.PlaybackPositionRepository;
import com.elmify.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaybackPositionService {

    private final PlaybackPositionRepository playbackPositionRepository;
    private final LectureRepository lectureRepository;
    private final UserRepository userRepository;

    /**
     * Get playback position for a specific lecture
     */
    public Optional<PlaybackPosition> getPosition(String clerkId, Long lectureId) {
        User user = userRepository.findByClerkId(clerkId)
                .orElseThrow(() -> new RuntimeException("User not found with clerkId: " + clerkId));
        return playbackPositionRepository.findByUserAndLectureId(user, lectureId);
    }

    /**
     * Get all playback positions for a user
     */
    public List<PlaybackPosition> getUserPositions(String clerkId) {
        User user = userRepository.findByClerkId(clerkId)
                .orElseThrow(() -> new RuntimeException("User not found with clerkId: " + clerkId));
        return playbackPositionRepository.findByUserWithLecture(user);
    }

    /**
     * Get lectures user can continue listening to
     * (positions where currentPosition > 0 and < duration)
     * Filters out premium lectures for non-premium users
     */
    public List<PlaybackPosition> getContinueListening(String clerkId) {
        User user = userRepository.findByClerkId(clerkId)
                .orElseThrow(() -> new RuntimeException("User not found with clerkId: " + clerkId));

        boolean userIsPremium = user.isPremium();

        return playbackPositionRepository.findContinueListening(user).stream()
                .filter(position -> {
                    if (userIsPremium) return true;
                    return !position.getLecture().isPremium();
                })
                .toList();
    }

    /**
     * Update or create playback position
     * Idempotent: creates new if doesn't exist, updates if it does
     */
    @Transactional
    public PlaybackPosition updatePosition(String clerkId, Long lectureId, Integer currentPosition) {
        // Find user
        User user = userRepository.findByClerkId(clerkId)
                .orElseThrow(() -> new RuntimeException("User not found with clerkId: " + clerkId));

        // Find existing position or create new one
        PlaybackPosition position = playbackPositionRepository
                .findByUserAndLectureId(user, lectureId)
                .orElseGet(() -> {
                    Lecture lecture = lectureRepository.findById(lectureId)
                            .orElseThrow(() -> new RuntimeException("Lecture not found with id: " + lectureId));
                    return new PlaybackPosition(user, lecture, currentPosition);
                });

        // Update position
        position.setCurrentPosition(currentPosition);
        return playbackPositionRepository.save(position);
    }

    /**
     * Delete playback position
     */
    @Transactional
    public void deletePosition(String clerkId, Long lectureId) {
        User user = userRepository.findByClerkId(clerkId)
                .orElseThrow(() -> new RuntimeException("User not found with clerkId: " + clerkId));
        playbackPositionRepository.deleteByUserAndLectureId(user, lectureId);
    }

    /**
     * Check if position exists
     */
    public boolean hasPosition(String clerkId, Long lectureId) {
        User user = userRepository.findByClerkId(clerkId)
                .orElseThrow(() -> new RuntimeException("User not found with clerkId: " + clerkId));
        return playbackPositionRepository.existsByUserAndLectureId(user, lectureId);
    }

    /**
     * Get recent lectures (most recently played)
     * Returns all playback positions ordered by last updated time
     * Filters out premium lectures for non-premium users
     * Useful for "Latest Lectures" feature
     */
    public List<PlaybackPosition> getRecentLectures(String clerkId, int limit) {
        User user = userRepository.findByClerkId(clerkId)
                .orElseThrow(() -> new RuntimeException("User not found with clerkId: " + clerkId));

        boolean userIsPremium = user.isPremium();

        // Get all positions ordered by lastUpdated DESC, filter premium content, and limit
        return playbackPositionRepository.findByUserWithLecture(user).stream()
                .filter(position -> {
                    // Allow all for premium users
                    if (userIsPremium) return true;
                    // Filter out premium lectures for non-premium users
                    return !position.getLecture().isPremium();
                })
                .limit(limit)
                .toList();
    }
}
