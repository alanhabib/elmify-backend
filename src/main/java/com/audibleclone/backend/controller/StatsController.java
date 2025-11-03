package com.audibleclone.backend.controller;

import com.audibleclone.backend.dto.*;
import com.audibleclone.backend.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for user listening statistics and progress tracking.
 * All endpoints require authentication and operate on the authenticated user's data.
 */
@RestController
@RequestMapping("/api/v1/stats")
@Tag(name = "Statistics", description = "User listening statistics and progress tracking")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
public class StatsController {

    private final StatsService statsService;

    /**
     * Get daily listening summary for authenticated user.
     * Returns today's listening progress toward the daily goal.
     *
     * GET /api/v1/stats/daily-summary
     *
     * @param authentication Spring Security authentication object
     * @return Daily summary with progress information
     */
    @GetMapping("/daily-summary")
    @Operation(
        summary = "Get Daily Summary",
        description = "Get today's listening progress and goal status for the authenticated user"
    )
    @ApiResponse(responseCode = "200", description = "Daily summary retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<DailySummaryDto> getDailySummary(Authentication authentication) {
        String clerkId = authentication.getName();
        log.debug("Fetching daily summary for user: {}", clerkId);

        DailySummaryDto summary = statsService.getDailySummary(clerkId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get user's current and best streaks.
     * Streaks are calculated based on consecutive days of meeting daily goals.
     *
     * GET /api/v1/stats/streaks
     *
     * @param authentication Spring Security authentication object
     * @return Streak information
     */
    @GetMapping("/streaks")
    @Operation(
        summary = "Get Streaks",
        description = "Get current streak and best streak for the authenticated user"
    )
    @ApiResponse(responseCode = "200", description = "Streaks retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<StreakDto> getStreaks(Authentication authentication) {
        String clerkId = authentication.getName();
        log.debug("Fetching streaks for user: {}", clerkId);

        StreakDto streaks = statsService.getStreaks(clerkId);
        return ResponseEntity.ok(streaks);
    }

    /**
     * Get weekly progress (last 7 days).
     * Returns day-by-day listening progress for the past week.
     *
     * GET /api/v1/stats/weekly-progress
     *
     * @param authentication Spring Security authentication object
     * @return Weekly progress with day-by-day breakdown
     */
    @GetMapping("/weekly-progress")
    @Operation(
        summary = "Get Weekly Progress",
        description = "Get listening progress for the last 7 days"
    )
    @ApiResponse(responseCode = "200", description = "Weekly progress retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<WeeklyProgressDto> getWeeklyProgress(Authentication authentication) {
        String clerkId = authentication.getName();
        log.debug("Fetching weekly progress for user: {}", clerkId);

        WeeklyProgressDto progress = statsService.getWeeklyProgress(clerkId);
        return ResponseEntity.ok(progress);
    }

    /**
     * Track listening activity.
     * Records listening progress for statistics and streak calculations.
     * This endpoint should be called periodically by the audio player (e.g., every 30 seconds).
     *
     * POST /api/v1/stats/track
     *
     * @param authentication Spring Security authentication object
     * @param trackDto Tracking data with lecture ID and play time
     * @return 200 OK if tracking was successful
     */
    @PostMapping("/track")
    @Operation(
        summary = "Track Listening",
        description = "Record listening activity for statistics. Call this periodically from the audio player."
    )
    @ApiResponse(responseCode = "200", description = "Listening activity tracked successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "User or lecture not found")
    public ResponseEntity<Void> trackListening(
            Authentication authentication,
            @Valid @RequestBody TrackListeningDto trackDto) {
        String clerkId = authentication.getName();
        log.debug("Tracking {} seconds for lecture {} for user {}",
            trackDto.playTimeSeconds(), trackDto.lectureId(), clerkId);

        statsService.trackListening(clerkId, trackDto);
        return ResponseEntity.ok().build();
    }
}
