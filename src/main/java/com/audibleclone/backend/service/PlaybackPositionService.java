package com.audibleclone.backend.service;

import com.audibleclone.backend.entity.Lecture;
import com.audibleclone.backend.entity.PlaybackPosition;
import com.audibleclone.backend.entity.User;
import com.audibleclone.backend.repository.LectureRepository;
import com.audibleclone.backend.repository.PlaybackPositionRepository;
import com.audibleclone.backend.repository.UserRepository;
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
     */
    public List<PlaybackPosition> getContinueListening(String clerkId) {
        User user = userRepository.findByClerkId(clerkId)
                .orElseThrow(() -> new RuntimeException("User not found with clerkId: " + clerkId));
        return playbackPositionRepository.findContinueListening(user);
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
     * Useful for "Latest Lectures" feature
     */
    public List<PlaybackPosition> getRecentLectures(String clerkId, int limit) {
        User user = userRepository.findByClerkId(clerkId)
                .orElseThrow(() -> new RuntimeException("User not found with clerkId: " + clerkId));

        // Get all positions ordered by lastUpdated DESC and limit the results
        return playbackPositionRepository.findByUserWithLecture(user).stream()
                .limit(limit)
                .toList();
    }
}
