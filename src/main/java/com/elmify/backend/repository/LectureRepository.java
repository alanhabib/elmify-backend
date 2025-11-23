package com.elmify.backend.repository;

import com.elmify.backend.entity.Lecture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Spring Data JPA repository for the Lecture entity.
 * Provides paginated, read-only methods for browsing content and a specific
 * method for updating the play count.
 */
@Repository
public interface LectureRepository extends JpaRepository<Lecture, Long> {

    // --- Core Browsing Methods (Paginated) ---

    /**
     * Finds a paginated list of lectures for a specific collection.
     * Spring Data JPA derives the query from the method name.
     *
     * @param collectionId The ID of the collection.
     * @param pageable Pagination information.
     * @return A Page of Lecture entities.
     */
    Page<Lecture> findByCollectionId(Long collectionId, Pageable pageable);

    /**
     * Finds a paginated list of lectures for a specific speaker.
     * Spring Data JPA derives the query from the method name based on the nested property.
     *
     * @param speakerId The ID of the speaker.
     * @param pageable Pagination information.
     * @return A Page of Lecture entities.
     */
    Page<Lecture> findBySpeakerId(Long speakerId, Pageable pageable);

    /**
     * Finds all lectures with eagerly loaded speaker and collection entities.
     * This prevents LazyInitializationException when accessing related entities outside of session.
     *
     * @param pageable Pagination information.
     * @return A Page of Lecture entities with speaker and collection loaded.
     */
    @Query("SELECT l FROM Lecture l LEFT JOIN FETCH l.speaker LEFT JOIN FETCH l.collection")
    Page<Lecture> findAllWithSpeakerAndCollection(Pageable pageable);

    /**
     * Finds lectures by collection ID with eagerly loaded speaker and collection entities.
     * Sorted by lectureNumber to maintain correct sequence based on R2 filename numbering.
     *
     * @param collectionId The ID of the collection.
     * @param pageable Pagination information.
     * @return A Page of Lecture entities with speaker and collection loaded, sorted by lectureNumber.
     */
    @Query(value = "SELECT l FROM Lecture l LEFT JOIN FETCH l.speaker LEFT JOIN FETCH l.collection WHERE l.collection.id = :collectionId ORDER BY l.lectureNumber ASC, l.title ASC",
           countQuery = "SELECT COUNT(l) FROM Lecture l WHERE l.collection.id = :collectionId")
    Page<Lecture> findByCollectionIdWithSpeakerAndCollection(@Param("collectionId") Long collectionId, Pageable pageable);

    /**
     * Finds lectures by speaker ID with eagerly loaded speaker and collection entities.
     *
     * @param speakerId The ID of the speaker.
     * @param pageable Pagination information.
     * @return A Page of Lecture entities with speaker and collection loaded.
     */
    @Query("SELECT l FROM Lecture l LEFT JOIN FETCH l.speaker LEFT JOIN FETCH l.collection WHERE l.speaker.id = :speakerId")
    Page<Lecture> findBySpeakerIdWithSpeakerAndCollection(@Param("speakerId") Long speakerId, Pageable pageable);


    // --- Search Method (Paginated and Corrected) ---

    /**
     * Searches for lectures where the search term matches the lecture title,
     * the associated speaker's name, or the associated collection's title.
     * This query is corrected to use proper entity relationships.
     *
     * @param searchTerm The term to search for.
     * @param pageable   Pagination information.
     * @return A Page of matching Lecture entities.
     */
    @Query("SELECT l FROM Lecture l WHERE " +
            "LOWER(l.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(l.speaker.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(l.collection.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Lecture> searchByTitleOrSpeakerOrCollectionName(
            @Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Finds lectures ordered by play count (most popular first) with eagerly loaded entities.
     * Used for trending/popular lectures.
     *
     * Note: The ORDER BY clause is intentionally in the query because Spring Data's Pageable
     * sorting doesn't work properly with JOIN FETCH queries. This is a known limitation.
     *
     * @param pageable Pagination information (sorting is handled in query).
     * @return A Page of Lecture entities ordered by popularity.
     */
    @Query(value = "SELECT l FROM Lecture l LEFT JOIN FETCH l.speaker LEFT JOIN FETCH l.collection ORDER BY l.playCount DESC",
           countQuery = "SELECT COUNT(l) FROM Lecture l")
    Page<Lecture> findAllOrderByPlayCountDesc(Pageable pageable);

    /**
     * Finds a single lecture by ID with eagerly loaded speaker and collection entities.
     * This prevents LazyInitializationException when accessing related entities outside of session.
     *
     * @param id The ID of the lecture.
     * @return The Lecture entity with speaker and collection loaded, or null if not found.
     */
    @Query("SELECT l FROM Lecture l LEFT JOIN FETCH l.speaker LEFT JOIN FETCH l.collection WHERE l.id = :id")
    Lecture findByIdWithSpeakerAndCollection(@Param("id") Long id);


    // --- Interaction Methods ---

    /**
     * Atomically increments the play count and updates the last played timestamp for a lecture.
     * Using @Modifying ensures this is an UPDATE query, which is highly efficient.
     *
     * @param lectureId The ID of the lecture to update.
     * @param timestamp The current time to set as last_played_at.
     */
    @Modifying
    @Transactional // Ensure this write operation is in its own transaction
    @Query("UPDATE Lecture l SET l.playCount = l.playCount + 1, l.lastPlayedAt = :timestamp WHERE l.id = :lectureId")
    void incrementPlayCount(@Param("lectureId") Long lectureId, @Param("timestamp") LocalDateTime timestamp);


    // --- Category-based Methods ---

    /**
     * Find lectures by category slug with eagerly loaded speaker and collection.
     * Includes lectures from premium and non-premium speakers.
     *
     * @param categorySlug The category slug.
     * @param pageable Pagination information.
     * @return A Page of Lecture entities in the category.
     */
    @Query(value = "SELECT DISTINCT l FROM Lecture l " +
            "LEFT JOIN FETCH l.speaker " +
            "LEFT JOIN FETCH l.collection " +
            "JOIN l.lectureCategories lc " +
            "WHERE lc.category.slug = :categorySlug " +
            "ORDER BY l.playCount DESC",
            countQuery = "SELECT COUNT(DISTINCT l) FROM Lecture l " +
                    "JOIN l.lectureCategories lc " +
                    "WHERE lc.category.slug = :categorySlug")
    Page<Lecture> findByCategorySlug(@Param("categorySlug") String categorySlug, Pageable pageable);

    /**
     * Find lectures by category slug, excluding premium speakers.
     *
     * @param categorySlug The category slug.
     * @param pageable Pagination information.
     * @return A Page of free Lecture entities in the category.
     */
    @Query(value = "SELECT DISTINCT l FROM Lecture l " +
            "LEFT JOIN FETCH l.speaker " +
            "LEFT JOIN FETCH l.collection " +
            "JOIN l.lectureCategories lc " +
            "WHERE lc.category.slug = :categorySlug " +
            "AND (l.speaker.isPremium = false OR l.speaker.isPremium IS NULL) " +
            "ORDER BY l.playCount DESC",
            countQuery = "SELECT COUNT(DISTINCT l) FROM Lecture l " +
                    "JOIN l.lectureCategories lc " +
                    "WHERE lc.category.slug = :categorySlug " +
                    "AND (l.speaker.isPremium = false OR l.speaker.isPremium IS NULL)")
    Page<Lecture> findByCategorySlugFreeOnly(@Param("categorySlug") String categorySlug, Pageable pageable);


    // --- Obsolete/Removed Methods ---
    // - All methods returning List<Lecture> have been replaced with paginated versions.
    // - All queries referencing the old text fields `l.speaker` or `l.collection` have been removed.
    // - Redundant methods like separate `findPublic...` queries are removed for simplicity;
    //   filtering by `isPublic` can be done in the service layer if needed.
}