package com.elmify.backend.controller;

import com.elmify.backend.dto.CollectionDto;
import com.elmify.backend.dto.LectureDto;
import com.elmify.backend.dto.PagedResponse;
import com.elmify.backend.dto.SpeakerDto;
import com.elmify.backend.service.CollectionService;
import com.elmify.backend.service.LectureService;
import com.elmify.backend.service.SpeakerService;
import com.elmify.backend.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Public REST controller for browsing speakers.
 * GET endpoints are public for guest browsing.
 */
@RestController
@RequestMapping("/api/v1/speakers")
@Tag(name = "Speakers", description = "Endpoints for browsing speakers")
@RequiredArgsConstructor
public class SpeakerController {

    private final SpeakerService speakerService;
    private final CollectionService collectionService;
    private final LectureService lectureService;
    private final StorageService storageService;

    @GetMapping
    @Operation(summary = "Get All Speakers (Paginated)",
            description = "Retrieves a paginated list of all speakers with mobile-optimized pagination metadata. Public endpoint.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved speakers")
    public ResponseEntity<PagedResponse<SpeakerDto>> getAllSpeakers(Pageable pageable) {
        Page<SpeakerDto> speakersPage = speakerService.getAllSpeakers(pageable);
        PagedResponse<SpeakerDto> response = PagedResponse.from(speakersPage);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Speaker by ID",
            description = "Retrieves a specific speaker by their ID. Public endpoint.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved speaker")
    @ApiResponse(responseCode = "404", description = "Speaker not found")
    public ResponseEntity<SpeakerDto> getSpeakerById(
            @Parameter(description = "The ID of the speaker") @PathVariable Long id) {
        return speakerService.getSpeakerById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/collections")
    @Operation(summary = "Get Collections by Speaker ID",
            description = "Retrieves all collections by a specific speaker. Public endpoint.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved collections")
    @ApiResponse(responseCode = "404", description = "Speaker not found")
    @Transactional(readOnly = true)
    public ResponseEntity<PagedResponse<CollectionDto>> getCollectionsBySpeakerId(
            @Parameter(description = "The ID of the speaker") @PathVariable Long id,
            Pageable pageable) {
        // First check if speaker exists
        if (!speakerService.getSpeakerById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }

        // Get collections using repository's findBySpeakerId method
        Page<CollectionDto> collections = collectionService.getCollectionsBySpeakerId(id, pageable)
                .map(collection -> CollectionDto.fromEntity(collection, storageService));
        PagedResponse<CollectionDto> response = PagedResponse.from(collections);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/lectures")
    @Operation(summary = "Get Lectures by Speaker ID",
            description = "Retrieves all lectures by a specific speaker across all collections. Public endpoint.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved lectures")
    @ApiResponse(responseCode = "404", description = "Speaker not found")
    @Transactional(readOnly = true)
    public ResponseEntity<PagedResponse<LectureDto>> getLecturesBySpeakerId(
            @Parameter(description = "The ID of the speaker") @PathVariable Long id,
            Pageable pageable) {
        // First check if speaker exists
        if (!speakerService.getSpeakerById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }

        // Get lectures using service's getLecturesBySpeakerId method
        Page<LectureDto> lectures = lectureService.getLecturesBySpeakerId(id, pageable)
                .map(lecture -> LectureDto.fromEntity(lecture, storageService));
        PagedResponse<LectureDto> response = PagedResponse.from(lectures);
        return ResponseEntity.ok(response);
    }
}