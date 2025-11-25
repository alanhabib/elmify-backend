package com.elmify.backend.service;

import com.elmify.backend.dto.PlaylistManifestRequest;
import com.elmify.backend.dto.PlaylistManifestResponse;
import com.elmify.backend.dto.PlaylistMetadata;
import com.elmify.backend.dto.TrackManifest;
import com.elmify.backend.entity.Lecture;
import com.elmify.backend.repository.LectureRepository;
import lombok.extern.slf4j.Slf4j;
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

    // Constructor without Redis (Redis support temporarily disabled)
    public PlaylistManifestService(
            LectureRepository lectureRepository,
            StorageService storageService) {
        this.lectureRepository = lectureRepository;
        this.storageService = storageService;

        log.info("⚠️ Playlist manifest service initialized WITHOUT Redis caching");
        log.info("💡 To enable Redis caching: uncomment Redis dependencies in pom.xml and redeploy");
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

        // Generate playlist identifier
        String playlistId = request.getCollectionId() != null
            ? request.getCollectionId()
            : request.getPlaylistType();

        log.info("🔄 Generating new manifest for playlist: {} (Redis caching disabled)", playlistId);

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
                .duration(lecture.getDuration() != null ? lecture.getDuration().longValue() : null)
                .build();

        } catch (Exception e) {
            log.error("❌ Failed to sign URL for lecture {}: {}", lecture.getId(), e.getMessage());
            throw new RuntimeException("Failed to generate signed URL for lecture: " + lecture.getId(), e);
        }
    }

}
