package com.elmify.backend.service;

import com.elmify.backend.entity.Collection;
import com.elmify.backend.repository.CollectionRepository;
import com.elmify.backend.repository.SpeakerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for retrieving Collection entities.
 * This service is designed for read-only operations to support the public API.
 *
 * Premium Filtering:
 * - All list endpoints filter out collections from premium speakers for non-premium users
 */
@Service
@Transactional(readOnly = true) // Service is read-only by default
@RequiredArgsConstructor
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final SpeakerRepository speakerRepository;
    private final StorageService storageService;
    private final PremiumFilterService premiumFilterService;
    // The SpeakerService dependency is removed as it was only used for creation logic.

    /**
     * Retrieves a paginated list of all collections.
     * Filters out premium collections for non-premium users.
     *
     * @param pageable Pagination information (page number, size, sort order).
     * @return A Page of Collection entities.
     */
    public Page<Collection> getAllCollections(Pageable pageable) {
        Page<Collection> collections = collectionRepository.findAll(pageable);
        return premiumFilterService.filterCollections(collections, pageable);
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
     * Filters out premium collections for non-premium users.
     *
     * @param speakerId The ID of the speaker.
     * @param pageable Pagination information (page number, size, sort order).
     * @return A Page of Collection entities.
     */
    public Page<Collection> getCollectionsBySpeakerId(Long speakerId, Pageable pageable) {
        // Convert List to Page manually since repository returns List
        List<Collection> collections = collectionRepository.findBySpeakerId(speakerId);

        // Filter premium collections
        List<Collection> filteredCollections = premiumFilterService.filterCollectionsList(collections);

        // Calculate pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredCollections.size());

        // Create Page from List
        return new org.springframework.data.domain.PageImpl<>(
            filteredCollections.subList(start, Math.min(end, filteredCollections.size())),
            pageable,
            filteredCollections.size()
        );
    }
}