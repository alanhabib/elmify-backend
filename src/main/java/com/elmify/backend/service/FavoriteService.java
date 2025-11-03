package com.elmify.backend.service;

import com.elmify.backend.entity.Favorite;
import com.elmify.backend.entity.Lecture;
import com.elmify.backend.entity.User;
import com.elmify.backend.repository.FavoriteRepository;
import com.elmify.backend.repository.LectureRepository;
import com.elmify.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing user favorites
 * Handles adding, removing, and retrieving favorites
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final LectureRepository lectureRepository;
    private final UserRepository userRepository;

    /**
     * Get all favorites for a user
     */
    public Page<Favorite> getUserFavorites(String clerkId, Pageable pageable) {
        User user = userRepository.findByClerkId(clerkId)
                .orElseThrow(() -> new RuntimeException("User not found with clerkId: " + clerkId));
        return favoriteRepository.findByUserWithLecture(user, pageable);
    }

    /**
     * Check if a lecture is favorited by a user
     */
    public boolean isFavorited(String clerkId, Long lectureId) {
        User user = userRepository.findByClerkId(clerkId)
                .orElseThrow(() -> new RuntimeException("User not found with clerkId: " + clerkId));
        return favoriteRepository.existsByUserAndLectureId(user, lectureId);
    }

    /**
     * Add a lecture to user's favorites
     */
    @Transactional
    public Favorite addFavorite(String clerkId, Long lectureId) {
        // Find user
        User user = userRepository.findByClerkId(clerkId)
                .orElseThrow(() -> new RuntimeException("User not found with clerkId: " + clerkId));

        // Check if already favorited
        if (favoriteRepository.existsByUserAndLectureId(user, lectureId)) {
            return favoriteRepository.findByUserAndLectureId(user, lectureId)
                    .orElseThrow(() -> new RuntimeException("Favorite exists but couldn't be retrieved"));
        }

        // Find the lecture
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new RuntimeException("Lecture not found with id: " + lectureId));

        // Create and save favorite
        Favorite favorite = new Favorite(user, lecture);
        return favoriteRepository.save(favorite);
    }

    /**
     * Remove a lecture from user's favorites
     */
    @Transactional
    public void removeFavorite(String clerkId, Long lectureId) {
        User user = userRepository.findByClerkId(clerkId)
                .orElseThrow(() -> new RuntimeException("User not found with clerkId: " + clerkId));
        favoriteRepository.deleteByUserAndLectureId(user, lectureId);
    }

    /**
     * Get total count of favorites for a user
     */
    public long getFavoriteCount(String clerkId) {
        User user = userRepository.findByClerkId(clerkId)
                .orElseThrow(() -> new RuntimeException("User not found with clerkId: " + clerkId));
        return favoriteRepository.countByUser(user);
    }
}
