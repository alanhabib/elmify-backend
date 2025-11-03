package com.elmify.backend.repository;

import com.elmify.backend.entity.Favorite;
import com.elmify.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Favorite entity
 * Provides methods for managing user favorites
 */
@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    /**
     * Find all favorites for a specific user with eagerly loaded lecture data
     */
    @Query("SELECT f FROM Favorite f LEFT JOIN FETCH f.lecture l LEFT JOIN FETCH l.speaker LEFT JOIN FETCH l.collection WHERE f.user = :user")
    Page<Favorite> findByUserWithLecture(@Param("user") User user, Pageable pageable);

    /**
     * Check if a user has favorited a specific lecture
     */
    boolean existsByUserAndLectureId(User user, Long lectureId);

    /**
     * Find a specific favorite by user and lecture
     */
    Optional<Favorite> findByUserAndLectureId(User user, Long lectureId);

    /**
     * Delete a favorite by user and lecture
     */
    void deleteByUserAndLectureId(User user, Long lectureId);

    /**
     * Count total favorites for a user
     */
    long countByUser(User user);
}
