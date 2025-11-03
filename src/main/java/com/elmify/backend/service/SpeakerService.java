package com.elmify.backend.service;

import com.elmify.backend.dto.SpeakerDto;
import com.elmify.backend.entity.Speaker;
import com.elmify.backend.repository.SpeakerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service layer for managing Speaker entities.
 * Handles business logic for creating, retrieving, updating, and deleting speakers.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SpeakerService {

    private final SpeakerRepository speakerRepository;
    private final StorageService storageService;

    /**
     * Creates a new speaker from a DTO.
     *
     * @param speakerDto DTO containing the details for the new speaker.
     * @return The created speaker, converted back to a DTO.
     */
    public SpeakerDto createSpeaker(SpeakerDto speakerDto) {
        // The service is responsible for converting the DTO to an entity
        Speaker speaker = new Speaker();
        speaker.setName(speakerDto.name());
        speaker.setImageUrl(speakerDto.imageUrl());
        speaker.setImageSmallUrl(speakerDto.imageSmallUrl());
        speaker.setIsPremium(speakerDto.isPremium());

        Speaker savedSpeaker = speakerRepository.save(speaker);
        return SpeakerDto.fromEntity(savedSpeaker);
    }

    /**
     * Retrieves a paginated list of all speakers.
     *
     * @param pageable Pagination information.
     * @return A Page of SpeakerDto objects.
     */
    @Transactional(readOnly = true)
    public Page<SpeakerDto> getAllSpeakers(Pageable pageable) {
        Page<Speaker> speakers = speakerRepository.findAll(pageable);
        return speakers.map(speaker -> SpeakerDto.fromEntity(speaker, storageService));
    }

    /**
     * Retrieves a single speaker by its ID.
     *
     * @param id The ID of the speaker to find.
     * @return An Optional containing the SpeakerDto if found.
     */
    @Transactional(readOnly = true)
    public Optional<SpeakerDto> getSpeakerById(Long id) {
        return speakerRepository.findById(id).map(speaker -> SpeakerDto.fromEntity(speaker, storageService));
    }
}