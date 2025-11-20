package com.elmify.backend.service;

import com.elmify.backend.entity.Lecture;
import com.elmify.backend.repository.LectureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service layer for retrieving and interacting with Lecture entities.
 * This service is primarily read-only, with specific methods for user interactions.
 *
 * Premium Filtering:
 * - All list endpoints filter out lectures from premium speakers for non-premium users
 * - Single lecture retrieval checks premium access
 */
@Service
@Transactional(readOnly = true) // Default to read-only transactions for performance
@RequiredArgsConstructor
public class LectureService {

    private final LectureRepository lectureRepository;
    private final PremiumFilterService premiumFilterService;

    /**
     * Retrieves a paginated list of all lectures with eagerly loaded related entities.
     * Filters out premium lectures for non-premium users.
     *
     * @param pageable Pagination information.
     * @return A Page of Lecture entities with speaker and collection loaded.
     */
    public Page<Lecture> getAllLectures(Pageable pageable) {
        Page<Lecture> lectures = lectureRepository.findAllWithSpeakerAndCollection(pageable);
        return premiumFilterService.filterLectures(lectures, pageable);
    }

    /**
     * Retrieves a single lecture by its ID with eagerly loaded speaker and collection.
     * Returns empty if lecture is from a premium speaker and user is not premium.
     *
     * @param id The ID of the lecture to find.
     * @return An Optional containing the Lecture entity if found and accessible.
     */
    public Optional<Lecture> getLectureById(Long id) {
        return Optional.ofNullable(lectureRepository.findByIdWithSpeakerAndCollection(id))
                .filter(premiumFilterService::canAccessLecture);
    }

    /**
     * Retrieves a paginated list of all lectures belonging to a specific collection with eagerly loaded related entities.
     * Filters out premium lectures for non-premium users.
     *
     * @param collectionId The ID of the collection.
     * @param pageable Pagination information.
     * @return A Page of Lecture entities with speaker and collection loaded.
     */
    public Page<Lecture> getLecturesByCollectionId(Long collectionId, Pageable pageable) {
        Page<Lecture> lectures = lectureRepository.findByCollectionIdWithSpeakerAndCollection(collectionId, pageable);
        return premiumFilterService.filterLectures(lectures, pageable);
    }

    /**
     * Retrieves a paginated list of all lectures belonging to a specific speaker.
     * Filters out premium lectures for non-premium users.
     *
     * @param speakerId The ID of the speaker.
     * @param pageable Pagination information.
     * @return A Page of Lecture entities.
     */
    public Page<Lecture> getLecturesBySpeakerId(Long speakerId, Pageable pageable) {
        Page<Lecture> lectures = lectureRepository.findBySpeakerIdWithSpeakerAndCollection(speakerId, pageable);
        return premiumFilterService.filterLectures(lectures, pageable);
    }

    /**
     * Retrieves trending lectures ordered by play count (most popular first).
     * Filters out premium lectures for non-premium users.
     *
     * @param pageable Pagination information.
     * @return A Page of Lecture entities ordered by popularity.
     */
    public Page<Lecture> getTrendingLectures(Pageable pageable) {
        Page<Lecture> lectures = lectureRepository.findAllOrderByPlayCountDesc(pageable);
        return premiumFilterService.filterLectures(lectures, pageable);
    }

    /**
     * Increments the play count for a specific lecture.
     * This is a write operation and requires its own transaction.
     * @param lectureId The ID of the lecture to update.
     */
    @Transactional // This annotation overrides the class-level readOnly=true
    public void incrementPlayCount(Long lectureId) {
        // The repository should handle the atomic update.
        // If it doesn't, we fetch, update, and save.
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new RuntimeException("Lecture not found with id: " + lectureId)); // Use custom exception

        lecture.setPlayCount(lecture.getPlayCount() + 1);
        lecture.setLastPlayedAt(LocalDateTime.now());
        lectureRepository.save(lecture);
    }
}