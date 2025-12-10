package com.elmify.backend.controller;

import com.elmify.backend.dto.PlaylistManifestRequest;
import com.elmify.backend.dto.PlaylistManifestResponse;
import com.elmify.backend.service.PlaylistManifestService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Playlist Manifest Controller - Production-grade bulk URL signing
 *
 * This controller implements Apple Podcasts/Spotify-style playlist manifest generation.
 * It bulk-signs audio URLs using Cloudflare R2 pre-signed URLs with Redis caching.
 *
 * Key Features:
 * - Parallel URL signing using virtual threads (Java 21+)
 * - Redis caching with 3.5-hour TTL
 * - 4-hour URL expiry (standard for streaming services)
 * - Rate limiting (30 requests/minute per user)
 * - Efficient batch processing
 */
@RestController
@RequestMapping("/api/v1/playlists")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Playlists", description = "Playlist manifest operations")
@CrossOrigin(origins = "*")
public class PlaylistManifestController {

    private final PlaylistManifestService playlistManifestService;

    // Rate limiting: 30 requests per minute per user
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Get playlist manifest with pre-signed URLs
     *
     * This endpoint returns a complete playlist manifest with pre-signed R2 URLs
     * for all requested lectures. Uses Redis caching for performance.
     *
     * Performance:
     * - Cached response: < 50ms
     * - Uncached (25 tracks): ~500ms
     * - Uncached (100 tracks): ~2s
     *
     * @param request Playlist manifest request with lecture IDs
     * @param jwt JWT authentication token
     * @return Playlist manifest with pre-signed URLs
     */
    @PostMapping("/manifest")
    @Operation(
        summary = "Get playlist manifest with pre-signed URLs",
        description = "Returns complete playlist manifest with pre-signed audio URLs for all lectures. " +
                      "Uses Redis caching for performance. URLs expire after 4 hours."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Playlist manifest generated successfully",
            content = @Content(schema = @Schema(implementation = PlaylistManifestResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing authentication token"
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Too many requests - Rate limit exceeded"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<PlaylistManifestResponse> getPlaylistManifest(
            @Valid @RequestBody PlaylistManifestRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        // Extract user ID from JWT (null for anonymous/guest users)
        // Best practice: Keep null for anonymous users to maintain type consistency
        String userId = jwt != null ? jwt.getSubject() : null;

        // Rate limiting check - use "anonymous" identifier only for bucketing
        Bucket bucket = getBucket(userId != null ? userId : "anonymous");
        if (!bucket.tryConsume(1)) {
            log.warn("⚠️ Rate limit exceeded for user: {}", userId);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        // Validate request
        if (request.getCollectionId() == null && request.getPlaylistType() == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            PlaylistManifestResponse manifest = playlistManifestService.getPlaylistManifest(request, userId);
            return ResponseEntity.ok(manifest);
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("❌ Failed to generate playlist manifest", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get or create rate limiting bucket for user
     */
    private Bucket getBucket(String userId) {
        return buckets.computeIfAbsent(userId != null ? userId : "anonymous", key -> {
            // 30 requests per minute
            Bandwidth limit = Bandwidth.classic(30, Refill.intervally(30, Duration.ofMinutes(1)));
            return Bucket.builder()
                .addLimit(limit)
                .build();
        });
    }
}
