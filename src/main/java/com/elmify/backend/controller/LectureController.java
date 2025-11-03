package com.elmify.backend.controller;

import com.elmify.backend.dto.LectureDto;
import com.elmify.backend.dto.PagedResponse;
import com.elmify.backend.entity.Lecture;
import com.elmify.backend.exception.ResourceNotFoundException;
import com.elmify.backend.service.LectureService;
import com.elmify.backend.service.StorageService; // You will need a StorageService
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for browsing lectures and accessing audio streams.
 */
@RestController
@RequestMapping("/api/v1/lectures")
@Tag(name = "Lectures", description = "Endpoints for browsing lectures and streaming audio")
@RequiredArgsConstructor
@Validated
public class LectureController {

    private static final Logger logger = LoggerFactory.getLogger(LectureController.class);

    private final LectureService lectureService;
    private final StorageService storageService;

    // --- Public Browsing Endpoints ---

    @GetMapping
    @Operation(summary = "Get All Lectures (Paginated)",
            description = "Retrieves a paginated list of all lectures with mobile-optimized pagination metadata. This is a public endpoint.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved lectures")
    public ResponseEntity<PagedResponse<LectureDto>> getAllLectures(Pageable pageable) {
        Page<LectureDto> lectureDtos = lectureService.getAllLectures(pageable)
                .map(lecture -> LectureDto.fromEntity(lecture, storageService));
        PagedResponse<LectureDto> response = PagedResponse.from(lectureDtos);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Lecture by ID",
            description = "Retrieves a specific lecture by its ID. This is a public endpoint.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved lecture")
    @ApiResponse(responseCode = "400", description = "Invalid lecture ID")
    @ApiResponse(responseCode = "404", description = "Lecture not found")
    public ResponseEntity<LectureDto> getLectureById(
            @Parameter(description = "Lecture ID", example = "1")
            @PathVariable @Positive Long id) {
        return lectureService.getLectureById(id)
                .map(lecture -> ResponseEntity.ok(LectureDto.fromEntity(lecture, storageService)))
                .orElseThrow(() -> new ResourceNotFoundException("Lecture", id));
    }

    @GetMapping("/collection/{collectionId}")
    @Operation(summary = "Get Lectures by Collection ID (Paginated)",
            description = "Retrieves a paginated list of lectures for a specific collection. Public endpoint.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved lectures")
    public ResponseEntity<PagedResponse<LectureDto>> getLecturesByCollection(
            @PathVariable Long collectionId, Pageable pageable) {
        Page<Lecture> lectures = lectureService.getLecturesByCollectionId(collectionId, pageable);
        Page<LectureDto> lectureDtos = lectures.map(lecture -> LectureDto.fromEntity(lecture, storageService));
        PagedResponse<LectureDto> response = PagedResponse.from(lectureDtos);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/speaker/{speakerId}")
    @Operation(summary = "Get Lectures by Speaker ID (Paginated)",
            description = "Retrieves a paginated list of lectures for a specific speaker. Public endpoint.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved lectures")
    public ResponseEntity<PagedResponse<LectureDto>> getLecturesBySpeaker(
            @PathVariable Long speakerId, Pageable pageable) {
        Page<Lecture> lectures = lectureService.getLecturesBySpeakerId(speakerId, pageable);
        Page<LectureDto> lectureDtos = lectures.map(lecture -> LectureDto.fromEntity(lecture, storageService));
        PagedResponse<LectureDto> response = PagedResponse.from(lectureDtos);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trending")
    @Operation(summary = "Get Most Popular Lectures",
            description = "Retrieves lectures ordered by play count (most popular first). Public endpoint.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved most popular lectures")
    public ResponseEntity<PagedResponse<LectureDto>> getTrendingLectures(
            @Parameter(description = "Maximum number of lectures to return", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) int limit) {
        Pageable pageable = Pageable.ofSize(limit);
        Page<Lecture> lectures = lectureService.getTrendingLectures(pageable);
        Page<LectureDto> lectureDtos = lectures.map(lecture -> LectureDto.fromEntity(lecture, storageService));
        PagedResponse<LectureDto> response = PagedResponse.from(lectureDtos);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/popular")
    @Operation(summary = "Get Most Popular Lectures",
            description = "Retrieves lectures ordered by play count (most popular first). Public endpoint.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved most popular lectures")
    public ResponseEntity<PagedResponse<LectureDto>> getPopularLectures(
            @Parameter(description = "Maximum number of lectures to return", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) int limit) {
        Pageable pageable = Pageable.ofSize(limit);
        Page<Lecture> lectures = lectureService.getTrendingLectures(pageable);
        Page<LectureDto> lectureDtos = lectures.map(lecture -> LectureDto.fromEntity(lecture, storageService));
        PagedResponse<LectureDto> response = PagedResponse.from(lectureDtos);
        return ResponseEntity.ok(response);
    }

    // --- Secure Streaming Endpoint ---

    @GetMapping("/{id}/stream-url")
    @Operation(summary = "Get Secure Stream URL",
            description = "Generates a secure, time-limited URL for streaming a lecture's audio. Requires authentication.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "URL generated successfully")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Lecture or audio file not found")
    @ApiResponse(responseCode = "500", description = "Failed to generate URL")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> getStreamUrl(@PathVariable Long id) {
        try {
            Lecture lecture = lectureService.getLectureById(id)
                    .orElseThrow(() -> new RuntimeException("Lecture not found"));

            // After getting the URL, increment the play count as an optimistic update.
            lectureService.incrementPlayCount(id);

            String presignedUrl = storageService.generatePresignedUrl(lecture.getFilePath());
            return ResponseEntity.ok(Map.of("url", presignedUrl));

        } catch (RuntimeException e) {
            logger.error("Failed to generate stream URL for lecture ID {}: {}", id, e.getMessage());
            // Check if the exception message indicates a "not found" scenario
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Could not generate stream URL"));
        }
    }
}