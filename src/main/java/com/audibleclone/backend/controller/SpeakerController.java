package com.audibleclone.backend.controller;

import com.audibleclone.backend.dto.CollectionDto;
import com.audibleclone.backend.dto.LectureDto;
import com.audibleclone.backend.dto.PagedResponse;
import com.audibleclone.backend.dto.SpeakerDto;
import com.audibleclone.backend.service.CollectionService;
import com.audibleclone.backend.service.LectureService;
import com.audibleclone.backend.service.SpeakerService;
import com.audibleclone.backend.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Secure, read-only REST controller for browsing speakers.
 * All endpoints require an authenticated user.
 */
@RestController
@RequestMapping("/api/v1/speakers")
@Tag(name = "Speakers", description = "Endpoints for browsing speakers (Authentication Required)")
@RequiredArgsConstructor
public class SpeakerController {

    private final SpeakerService speakerService;
    private final CollectionService collectionService;
    private final LectureService lectureService;
    private final StorageService storageService;

    @GetMapping
    @Operation(summary = "Get All Speakers (Paginated)",
            description = "Retrieves a paginated list of all speakers with mobile-optimized pagination metadata. Requires authentication.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved speakers")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @SecurityRequirement(name = "bearerAuth") // This documents the security requirement in Swagger
    @PreAuthorize("isAuthenticated()") // This enforces the security rule
    public ResponseEntity<PagedResponse<SpeakerDto>> getAllSpeakers(Pageable pageable) {
        Page<SpeakerDto> speakersPage = speakerService.getAllSpeakers(pageable);
        PagedResponse<SpeakerDto> response = PagedResponse.from(speakersPage);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Speaker by ID",
            description = "Retrieves a specific speaker by their ID. Requires authentication.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved speaker")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Speaker not found")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SpeakerDto> getSpeakerById(
            @Parameter(description = "The ID of the speaker") @PathVariable Long id) {
        return speakerService.getSpeakerById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/collections")
    @Operation(summary = "Get Collections by Speaker ID",
            description = "Retrieves all collections by a specific speaker. Requires authentication.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved collections")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Speaker not found")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
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
            description = "Retrieves all lectures by a specific speaker across all collections. Requires authentication.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved lectures")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Speaker not found")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
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