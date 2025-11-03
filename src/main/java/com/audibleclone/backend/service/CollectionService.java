package com.audibleclone.backend.service;

import com.audibleclone.backend.dto.CollectionDto;
import com.audibleclone.backend.entity.Collection;
import com.audibleclone.backend.entity.Speaker;
import com.audibleclone.backend.repository.CollectionRepository;
import com.audibleclone.backend.repository.SpeakerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service layer for retrieving Collection entities.
 * This service is designed for read-only operations to support the public API.
 */
@Service
@Transactional(readOnly = true) // Service is read-only by default
@RequiredArgsConstructor
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final SpeakerRepository speakerRepository;
    private final StorageService storageService;
    // The SpeakerService dependency is removed as it was only used for creation logic.

    /**
     * Retrieves a paginated list of all collections.
     * This method now correctly accepts a Pageable object from the controller.
     *
     * @param pageable Pagination information (page number, size, sort order).
     * @return A Page of Collection entities.
     */
    public Page<Collection> getAllCollections(Pageable pageable) {
        // We use the standard, built-in findAll method which supports pagination.
        return collectionRepository.findAll(pageable);
    }

    /**
     * Retrieves a single collection by its ID. The repository method should be configured
     * to fetch related entities like the Speaker to avoid lazy loading issues.
     *
     * @param id The ID of the collection to find.
     * @return An Optional containing the Collection entity if found.
     */
    public Optional<Collection> getCollectionById(Long id) {
        // Assuming the repository method handles fetching the speaker as well
        return collectionRepository.findById(id);
    }

    /**
     * Retrieves a paginated list of collections by speaker ID.
     *
     * @param speakerId The ID of the speaker.
     * @param pageable Pagination information (page number, size, sort order).
     * @return A Page of Collection entities.
     */
    public Page<Collection> getCollectionsBySpeakerId(Long speakerId, Pageable pageable) {
        // Convert List to Page manually since repository returns List
        var collections = collectionRepository.findBySpeakerId(speakerId);

        // Calculate pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), collections.size());

        // Create Page from List
        return new org.springframework.data.domain.PageImpl<>(
            collections.subList(start, Math.min(end, collections.size())),
            pageable,
            collections.size()
        );
    }
}