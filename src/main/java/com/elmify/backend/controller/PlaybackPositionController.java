package com.elmify.backend.controller;

import com.elmify.backend.dto.PlaybackPositionDto;
import com.elmify.backend.dto.PlaybackPositionWithLectureDto;
import com.elmify.backend.entity.PlaybackPosition;
import com.elmify.backend.service.PlaybackPositionService;
import com.elmify.backend.service.StorageService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Playback Position Controller
 * Manages user playback positions for lectures
 * All endpoints require authentication
 */
@RestController
@RequestMapping("/api/v1/playback")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
public class PlaybackPositionController {

    private final PlaybackPositionService playbackPositionService;
    private final StorageService storageService;

    /**
     * Get playback position for a specific lecture
     * GET /api/v1/playback/{lectureId}
     */
    @GetMapping("/{lectureId}")
    public ResponseEntity<PlaybackPositionDto> getPosition(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long lectureId) {
        String userId = jwt.getSubject();

        return playbackPositionService.getPosition(userId, lectureId)
                .map(position -> ResponseEntity.ok(PlaybackPositionDto.fromEntity(position)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all playback positions for the authenticated user
     * GET /api/v1/playback (no path, just the base)
     */
    @GetMapping
    public ResponseEntity<List<PlaybackPositionDto>> getUserPositions(
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        List<PlaybackPosition> positions = playbackPositionService.getUserPositions(userId);
        List<PlaybackPositionDto> positionDtos = positions.stream()
                .map(PlaybackPositionDto::fromEntity)
                .toList();

        return ResponseEntity.ok(positionDtos);
    }

    /**
     * Get lectures the user can continue listening to
     * GET /api/v1/playback/continue-listening
     */
    @GetMapping("/continue-listening")
    public ResponseEntity<List<PlaybackPositionDto>> getContinueListening(
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        List<PlaybackPosition> positions = playbackPositionService.getContinueListening(userId);
        List<PlaybackPositionDto> positionDtos = positions.stream()
                .map(PlaybackPositionDto::fromEntity)
                .toList();

        return ResponseEntity.ok(positionDtos);
    }

    /**
     * Get recent lectures (most recently played)
     * GET /api/v1/playback/recent
     * Returns playback positions with full lecture details for "Latest Lectures" feature
     */
    @GetMapping("/recent")
    public ResponseEntity<List<PlaybackPositionWithLectureDto>> getRecentLectures(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "10") int limit) {
        String userId = jwt.getSubject();
        List<PlaybackPosition> positions = playbackPositionService.getRecentLectures(userId, limit);
        List<PlaybackPositionWithLectureDto> positionDtos = positions.stream()
                .map(position -> PlaybackPositionWithLectureDto.fromEntity(position, storageService))
                .toList();

        return ResponseEntity.ok(positionDtos);
    }

    /**
     * Update playback position for a lecture
     * PUT /api/v1/playback/{lectureId}
     * Request body: { "currentPosition": 12345 }
     */
    @PutMapping("/{lectureId}")
    public ResponseEntity<PlaybackPositionDto> updatePosition(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long lectureId,
            @RequestBody Map<String, Integer> request) {
        String userId = jwt.getSubject();
        Integer currentPosition = request.get("currentPosition");

        if (currentPosition == null || currentPosition < 0) {
            return ResponseEntity.badRequest().build();
        }

        PlaybackPosition position = playbackPositionService.updatePosition(userId, lectureId, currentPosition);
        return ResponseEntity.ok(PlaybackPositionDto.fromEntity(position));
    }

    /**
     * Delete playback position for a lecture
     * DELETE /api/v1/playback/{lectureId}
     */
    @DeleteMapping("/{lectureId}")
    public ResponseEntity<Void> deletePosition(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long lectureId) {
        String userId = jwt.getSubject();
        playbackPositionService.deletePosition(userId, lectureId);
        return ResponseEntity.noContent().build();
    }
}
