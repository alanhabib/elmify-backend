package com.elmify.backend.service;

import com.elmify.backend.dto.SpeakerDto;
import com.elmify.backend.entity.Speaker;
import com.elmify.backend.entity.User;
import com.elmify.backend.repository.SpeakerRepository;
import com.elmify.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final UserRepository userRepository;

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
        speaker.setBio(speakerDto.bio());
        speaker.setImageUrl(speakerDto.imageUrl());
        speaker.setImageSmallUrl(speakerDto.imageSmallUrl());
        speaker.setIsPremium(speakerDto.isPremium());

        Speaker savedSpeaker = speakerRepository.save(speaker);
        return SpeakerDto.fromEntity(savedSpeaker);
    }

    /**
     * Retrieves a paginated list of all speakers.
     * Filters premium speakers based on user's subscription status.
     *
     * @param pageable Pagination information.
     * @return A Page of SpeakerDto objects filtered by user's premium status.
     */
    @Transactional(readOnly = true)
    public Page<SpeakerDto> getAllSpeakers(Pageable pageable) {
        Page<Speaker> speakers = speakerRepository.findAll(pageable);

        // Check if current user is premium
        boolean userIsPremium = isCurrentUserPremium();

        // If user is NOT premium, filter out premium speakers
        if (!userIsPremium) {
            List<Speaker> filteredSpeakers = speakers.getContent().stream()
                    .filter(speaker -> speaker.getIsPremium() == null || !speaker.getIsPremium())
                    .collect(Collectors.toList());

            speakers = new PageImpl<>(filteredSpeakers, pageable, filteredSpeakers.size());
        }

        return speakers.map(speaker -> SpeakerDto.fromEntity(speaker, storageService));
    }

    /**
     * Check if the currently authenticated user has premium access.
     *
     * @return true if user is premium, false otherwise
     */
    private boolean isCurrentUserPremium() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String clerkId = authentication.getName();
        Optional<User> userOpt = userRepository.findByClerkId(clerkId);

        return userOpt.map(User::isPremium).orElse(false);
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