package com.elmify.backend.service;

import com.elmify.backend.entity.Collection;
import com.elmify.backend.entity.Lecture;
import com.elmify.backend.entity.User;
import com.elmify.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Centralized service for handling premium content filtering.
 *
 * Best Practice: Single Responsibility Principle
 * - All premium filtering logic is centralized here
 * - Easy to maintain and test
 * - Consistent behavior across all services
 *
 * Premium Content Rules:
 * - Premium speakers cascade to their collections and lectures
 * - Non-premium users cannot see premium speakers, collections, or lectures
 * - Premium users see all content
 */
@Service
@RequiredArgsConstructor
public class PremiumFilterService {

    private final UserRepository userRepository;

    /**
     * Check if the currently authenticated user has premium access.
     *
     * @return true if user is premium, false otherwise
     */
    public boolean isCurrentUserPremium() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String clerkId = authentication.getName();
        Optional<User> userOpt = userRepository.findByClerkId(clerkId);

        return userOpt.map(User::isPremium).orElse(false);
    }

    /**
     * Filter a Page of lectures based on user's premium status.
     * Filters out lectures from premium speakers if user is not premium.
     *
     * @param lectures The page of lectures to filter
     * @param pageable Pagination information
     * @return Filtered page of lectures
     */
    public Page<Lecture> filterLectures(Page<Lecture> lectures, Pageable pageable) {
        if (isCurrentUserPremium()) {
            return lectures;
        }

        List<Lecture> filteredLectures = lectures.getContent().stream()
                .filter(lecture -> !lecture.isPremium())
                .collect(Collectors.toList());

        return new PageImpl<>(filteredLectures, pageable, filteredLectures.size());
    }

    /**
     * Filter a list of lectures based on user's premium status.
     * Filters out lectures from premium speakers if user is not premium.
     *
     * @param lectures The list of lectures to filter
     * @return Filtered list of lectures
     */
    public List<Lecture> filterLecturesList(List<Lecture> lectures) {
        if (isCurrentUserPremium()) {
            return lectures;
        }

        return lectures.stream()
                .filter(lecture -> !lecture.isPremium())
                .collect(Collectors.toList());
    }

    /**
     * Filter a Page of collections based on user's premium status.
     * Filters out collections from premium speakers if user is not premium.
     *
     * @param collections The page of collections to filter
     * @param pageable Pagination information
     * @return Filtered page of collections
     */
    public Page<Collection> filterCollections(Page<Collection> collections, Pageable pageable) {
        if (isCurrentUserPremium()) {
            return collections;
        }

        List<Collection> filteredCollections = collections.getContent().stream()
                .filter(collection -> !collection.isPremium())
                .collect(Collectors.toList());

        return new PageImpl<>(filteredCollections, pageable, filteredCollections.size());
    }

    /**
     * Filter a list of collections based on user's premium status.
     * Filters out collections from premium speakers if user is not premium.
     *
     * @param collections The list of collections to filter
     * @return Filtered list of collections
     */
    public List<Collection> filterCollectionsList(List<Collection> collections) {
        if (isCurrentUserPremium()) {
            return collections;
        }

        return collections.stream()
                .filter(collection -> !collection.isPremium())
                .collect(Collectors.toList());
    }

    /**
     * Check if a specific lecture should be accessible to the current user.
     * Returns false if the lecture is premium and the user is not premium.
     *
     * @param lecture The lecture to check
     * @return true if accessible, false otherwise
     */
    public boolean canAccessLecture(Lecture lecture) {
        if (lecture == null) {
            return false;
        }

        if (isCurrentUserPremium()) {
            return true;
        }

        return !lecture.isPremium();
    }

    /**
     * Check if a specific collection should be accessible to the current user.
     * Returns false if the collection is premium and the user is not premium.
     *
     * @param collection The collection to check
     * @return true if accessible, false otherwise
     */
    public boolean canAccessCollection(Collection collection) {
        if (collection == null) {
            return false;
        }

        if (isCurrentUserPremium()) {
            return true;
        }

        return !collection.isPremium();
    }
}
