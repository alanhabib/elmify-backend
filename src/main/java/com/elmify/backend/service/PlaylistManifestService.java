package com.elmify.backend.service;

import com.elmify.backend.dto.PlaylistManifestRequest;
import com.elmify.backend.dto.PlaylistManifestResponse;
import com.elmify.backend.dto.PlaylistMetadata;
import com.elmify.backend.dto.TrackManifest;
import com.elmify.backend.entity.Lecture;
import com.elmify.backend.repository.LectureRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Service for generating playlist manifests with pre-signed URLs
 *
 * This service handles bulk URL signing and caching for playlist manifests.
 * Uses Redis for caching and parallel processing for URL signing.
 */
@Service
@Slf4j
public class PlaylistManifestService {

    private final LectureRepository lectureRepository;
    private final StorageService storageService;
    private final RedisTemplate<String, PlaylistManifestResponse> redisTemplate;

    // Constructor with optional Redis
    public PlaylistManifestService(
            LectureRepository lectureRepository,
            StorageService storageService,
            @Autowired(required = false) RedisTemplate<String, PlaylistManifestResponse> redisTemplate) {
        this.lectureRepository = lectureRepository;
        this.storageService = storageService;
        this.redisTemplate = redisTemplate;

        if (redisTemplate == null) {
            log.warn("⚠️ Redis not configured - Playlist manifest caching is DISABLED. Performance will be degraded.");
        } else {
            log.info("✅ Redis caching enabled for playlist manifests");
        }
    }

    // Virtual thread executor for parallel URL signing (Java 21+)
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    // Cache TTL: 3.5 hours (shorter than URL expiry for safety margin)
    private static final Duration CACHE_TTL = Duration.ofMinutes(210);

    // URL expiry: 4 hours (industry standard for streaming)
    private static final Duration URL_EXPIRY = Duration.ofHours(4);

    /**
     * Get playlist manifest with pre-signed URLs
     *
     * @param request Playlist manifest request
     * @param userId Current user ID for caching
     * @return Playlist manifest with pre-signed URLs
     */
    public PlaylistManifestResponse getPlaylistManifest(PlaylistManifestRequest request, String userId) {
        log.info("📋 Playlist manifest request: collectionId={}, playlistType={}, tracks={}, userId={}",
                 request.getCollectionId(), request.getPlaylistType(),
                 request.getLectureIds().size(), userId);

        // Generate cache key
        String playlistId = request.getCollectionId() != null
            ? request.getCollectionId()
            : request.getPlaylistType();
        String cacheKey = generateCacheKey(playlistId, userId);

        // Try to get from cache (only if Redis is available)
        if (redisTemplate != null) {
            try {
                PlaylistManifestResponse cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null && isManifestValid(cached)) {
                    log.info("✅ Cache HIT for playlist: {}", playlistId);
                    cached.getMetadata().setCached(true);
                    return cached;
                }
            } catch (Exception e) {
                log.warn("⚠️ Redis cache read failed, continuing without cache: {}", e.getMessage());
            }
        }

        log.info("🔄 Cache MISS for playlist: {} - Generating new manifest", playlistId);

        // Fetch lectures from database
        List<Lecture> lectures = lectureRepository.findAllById(
            request.getLectureIds().stream()
                .map(Long::parseLong)
                .collect(Collectors.toList())
        );

        // Validate lectures exist
        if (lectures.size() != request.getLectureIds().size()) {
            log.warn("❌ Some lectures not found. Requested: {}, Found: {}",
                     request.getLectureIds().size(), lectures.size());
            throw new IllegalArgumentException("Some lectures were not found");
        }

        // Create a map to preserve the order from request
        Map<Long, Lecture> lectureMap = lectures.stream()
            .collect(Collectors.toMap(Lecture::getId, l -> l));

        // Generate manifest with parallel URL signing (preserving order)
        PlaylistManifestResponse manifest = generateManifest(
            playlistId,
            request.getLectureIds().stream()
                .map(id -> lectureMap.get(Long.parseLong(id)))
                .collect(Collectors.toList())
        );

        // Cache the result (only if Redis is available)
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, manifest, CACHE_TTL);
                log.info("💾 Cached manifest for playlist: {} (TTL: {} minutes)", playlistId, CACHE_TTL.toMinutes());
            } catch (Exception e) {
                log.warn("⚠️ Redis cache write failed, continuing without cache: {}", e.getMessage());
            }
        }

        manifest.getMetadata().setCached(false);
        return manifest;
    }

    /**
     * Generate playlist manifest with parallel URL signing
     */
    private PlaylistManifestResponse generateManifest(String playlistId, List<Lecture> lectures) {
        Instant startTime = Instant.now();
        Instant expiresAt = Instant.now().plus(URL_EXPIRY);

        // Parallel URL signing using virtual threads
        List<CompletableFuture<TrackManifest>> futures = lectures.stream()
            .map(lecture -> CompletableFuture.supplyAsync(
                () -> signLectureUrl(lecture, expiresAt),
                executor
            ))
            .toList();

        // Wait for all signatures to complete
        List<TrackManifest> tracks = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());

        // Calculate metadata
        long totalDuration = lectures.stream()
            .mapToLong(l -> l.getDuration() != null ? l.getDuration() : 0L)
            .sum();

        PlaylistMetadata metadata = PlaylistMetadata.builder()
            .totalTracks(tracks.size())
            .totalDuration(totalDuration)
            .generatedAt(Instant.now())
            .expiresAt(expiresAt)
            .cached(false)
            .build();

        long elapsedMs = Duration.between(startTime, Instant.now()).toMillis();
        log.info("✅ Generated manifest for {} tracks in {}ms", tracks.size(), elapsedMs);

        return PlaylistManifestResponse.builder()
            .collectionId(playlistId)
            .tracks(tracks)
            .metadata(metadata)
            .build();
    }

    /**
     * Sign a single lecture URL using R2 pre-signed URL
     */
    private TrackManifest signLectureUrl(Lecture lecture, Instant expiresAt) {
        try {
            // Generate R2 pre-signed URL
            String signedUrl = storageService.generatePresignedUrl(lecture.getFilePath());

            return TrackManifest.builder()
                .lectureId(lecture.getId().toString())
                .audioUrl(signedUrl)
                .expiresAt(expiresAt)
                .duration(lecture.getDuration())
                .build();

        } catch (Exception e) {
            log.error("❌ Failed to sign URL for lecture {}: {}", lecture.getId(), e.getMessage());
            throw new RuntimeException("Failed to generate signed URL for lecture: " + lecture.getId(), e);
        }
    }

    /**
     * Generate Redis cache key
     */
    private String generateCacheKey(String playlistId, String userId) {
        // Include userId for user-specific playlists (favorites, history)
        // For public collections, userId can be null
        return String.format("playlist:manifest:%s:%s", playlistId, userId != null ? userId : "public");
    }

    /**
     * Check if cached manifest is still valid
     */
    private boolean isManifestValid(PlaylistManifestResponse manifest) {
        if (manifest == null || manifest.getMetadata() == null) {
            return false;
        }

        // Check if URLs are still valid (with 5-minute safety buffer)
        Instant expiryThreshold = Instant.now().plus(5, ChronoUnit.MINUTES);
        return manifest.getMetadata().getExpiresAt().isAfter(expiryThreshold);
    }

    /**
     * Clear cache for a specific collection or all
     */
    public void clearCache(String collectionId) {
        if (redisTemplate == null) {
            log.warn("⚠️ Redis not available - cache clear skipped");
            return;
        }

        try {
            if (collectionId != null) {
                redisTemplate.delete("playlist:manifest:" + collectionId + ":*");
                log.info("🗑️ Cleared cache for collection {}", collectionId);
            } else {
                redisTemplate.delete("playlist:manifest:*");
                log.info("🗑️ Cleared all playlist manifest cache");
            }
        } catch (Exception e) {
            log.error("❌ Failed to clear cache: {}", e.getMessage());
        }
    }
}
