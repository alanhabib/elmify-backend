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
 */
@Service
@Transactional(readOnly = true) // Default to read-only transactions for performance
@RequiredArgsConstructor
public class LectureService {

    private final LectureRepository lectureRepository;

    /**
     * Retrieves a paginated list of all lectures with eagerly loaded related entities.
     * @param pageable Pagination information.
     * @return A Page of Lecture entities with speaker and collection loaded.
     */
    public Page<Lecture> getAllLectures(Pageable pageable) {
        return lectureRepository.findAllWithSpeakerAndCollection(pageable);
    }

    /**
     * Retrieves a single lecture by its ID with eagerly loaded speaker and collection.
     * @param id The ID of the lecture to find.
     * @return An Optional containing the Lecture entity if found.
     */
    public Optional<Lecture> getLectureById(Long id) {
        return Optional.ofNullable(lectureRepository.findByIdWithSpeakerAndCollection(id));
    }

    /**
     * Retrieves a paginated list of all lectures belonging to a specific collection with eagerly loaded related entities.
     * @param collectionId The ID of the collection.
     * @param pageable Pagination information.
     * @return A Page of Lecture entities with speaker and collection loaded.
     */
    public Page<Lecture> getLecturesByCollectionId(Long collectionId, Pageable pageable) {
        return lectureRepository.findByCollectionIdWithSpeakerAndCollection(collectionId, pageable);
    }

    /**
     * Retrieves a paginated list of all lectures belonging to a specific speaker.
     * @param speakerId The ID of the speaker.
     * @param pageable Pagination information.
     * @return A Page of Lecture entities.
     */
    public Page<Lecture> getLecturesBySpeakerId(Long speakerId, Pageable pageable) {
        return lectureRepository.findBySpeakerIdWithSpeakerAndCollection(speakerId, pageable);
    }

    /**
     * Retrieves trending lectures ordered by play count (most popular first).
     * @param pageable Pagination information.
     * @return A Page of Lecture entities ordered by popularity.
     */
    public Page<Lecture> getTrendingLectures(Pageable pageable) {
        return lectureRepository.findAllOrderByPlayCountDesc(pageable);
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