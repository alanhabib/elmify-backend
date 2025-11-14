package com.elmify.backend.service;

import com.elmify.backend.entity.Favorite;
import com.elmify.backend.entity.Lecture;
import com.elmify.backend.entity.User;
import com.elmify.backend.repository.FavoriteRepository;
import com.elmify.backend.repository.LectureRepository;
import com.elmify.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing user favorites.
 * Handles adding, removing, and retrieving favorites.
 *
 * Premium Filtering:
 * - Favorites list filters out lectures from premium speakers for non-premium users
 * - Non-premium users can only add non-premium lectures to favorites
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final LectureRepository lectureRepository;
    private final UserRepository userRepository;
    private final PremiumFilterService premiumFilterService;

    /**
     * Get all favorites for a user.
     * Filters out favorites for premium lectures if user is not premium.
     */
    public Page<Favorite> getUserFavorites(String clerkId, Pageable pageable) {
        User user = userRepository.findByClerkId(clerkId)
                .orElseThrow(() -> new RuntimeException("User not found with clerkId: " + clerkId));

        Page<Favorite> favorites = favoriteRepository.findByUserWithLecture(user, pageable);

        // If user is premium, return all favorites
        if (premiumFilterService.isCurrentUserPremium()) {
            return favorites;
        }

        // Filter out favorites for premium lectures
        List<Favorite> filteredFavorites = favorites.getContent().stream()
                .filter(favorite -> !favorite.getLecture().isPremium())
                .collect(Collectors.toList());

        return new PageImpl<>(filteredFavorites, pageable, filteredFavorites.size());
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
     * Add a lecture to user's favorites.
     * Prevents non-premium users from favoriting premium lectures.
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

        // Check if user can access this lecture (premium check)
        if (!premiumFilterService.canAccessLecture(lecture)) {
            throw new RuntimeException("Cannot favorite premium content without premium access");
        }

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
