package com.elmify.backend.service;

import com.elmify.backend.config.StreamingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * Service for handling audio streaming operations.
 * Provides optimized streaming with range request support for smooth playback
 * on mobile devices and web browsers.
 */
@Service
public class AudioStreamingService {

    private static final Logger logger = LoggerFactory.getLogger(AudioStreamingService.class);
    private static final String AUDIO_MPEG_CONTENT_TYPE = "audio/mpeg";
    private static final String BYTES_RANGE_PREFIX = "bytes=";

    private final StorageService storageService;
    private final StreamingProperties streamingProperties;

    public AudioStreamingService(StorageService storageService, StreamingProperties streamingProperties) {
        this.storageService = storageService;
        this.streamingProperties = streamingProperties;
    }

    /**
     * Stream audio file with support for range requests (HTTP 206 Partial Content).
     * This enables seeking, resumable downloads, and efficient mobile streaming.
     *
     * @param objectKey The R2 storage object key (file path)
     * @param rangeHeader Optional HTTP Range header (e.g., "bytes=0-1023")
     * @return ResponseEntity with appropriate status code and streaming resource
     */
    public ResponseEntity<Resource> streamAudio(String objectKey, String rangeHeader) {
        try {
            // Get file metadata to determine size and content type
            var metadata = storageService.getObjectMetadata(objectKey);
            long fileSize = metadata.size();
            String contentType = metadata.contentType() != null ? metadata.contentType() : AUDIO_MPEG_CONTENT_TYPE;

            if (streamingProperties.getEnableDetailedLogging()) {
                logger.debug("Streaming request for: {} (size: {} bytes, range: {})",
                    objectKey, fileSize, rangeHeader);
            }

            // Handle range requests for partial content delivery
            if (rangeHeader != null && rangeHeader.startsWith(BYTES_RANGE_PREFIX)) {
                return handleRangeRequest(objectKey, rangeHeader, fileSize, contentType);
            }

            // Stream full content
            return handleFullRequest(objectKey, fileSize, contentType);

        } catch (Exception e) {
            logger.error("Failed to stream audio for key: {}", objectKey, e);
            throw new RuntimeException("Failed to stream audio", e);
        }
    }

    /**
     * Handle HTTP range request (RFC 7233) for partial content delivery.
     * Supports seeking and chunked streaming for large files.
     */
    private ResponseEntity<Resource> handleRangeRequest(
            String objectKey,
            String rangeHeader,
            long fileSize,
            String contentType) {

        RangeSpec range = parseRangeHeader(rangeHeader, fileSize);

        if (streamingProperties.getEnableDetailedLogging()) {
            logger.debug("Serving range: bytes={}-{}/{} (requested: {})",
                range.start, range.end, fileSize, rangeHeader);
        }

        // Get partial content from R2 storage
        ResponseInputStream<GetObjectResponse> rangedStream =
            storageService.getObjectStreamRange(objectKey, range.start, range.end);

        Resource resource = new InputStreamResource(rangedStream);
        long contentLength = range.end - range.start + 1;

        HttpHeaders headers = buildStreamingHeaders(contentType, true);
        headers.add(HttpHeaders.CONTENT_RANGE,
            String.format("bytes %d-%d/%d", range.start, range.end, fileSize));
        headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));

        return new ResponseEntity<>(resource, headers, HttpStatus.PARTIAL_CONTENT);
    }

    /**
     * Handle full content request (no range header).
     */
    private ResponseEntity<Resource> handleFullRequest(
            String objectKey,
            long fileSize,
            String contentType) {

        if (streamingProperties.getEnableDetailedLogging()) {
            logger.debug("Serving full content: {} ({} bytes)", objectKey, fileSize);
        }

        ResponseInputStream<GetObjectResponse> fullStream =
            storageService.getObjectStream(objectKey);

        Resource resource = new InputStreamResource(fullStream);

        HttpHeaders headers = buildStreamingHeaders(contentType, true);
        headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize));

        return ResponseEntity.ok()
            .headers(headers)
            .body(resource);
    }

    /**
     * Parse HTTP Range header and apply chunk size limits.
     * Format: "bytes=start-end" or "bytes=start-"
     */
    private RangeSpec parseRangeHeader(String rangeHeader, long fileSize) {
        String[] ranges = rangeHeader.substring(BYTES_RANGE_PREFIX.length()).split("-");

        long start = ranges[0].isEmpty() ? 0 : Long.parseLong(ranges[0]);
        long requestedEnd = ranges.length > 1 && !ranges[1].isEmpty()
            ? Long.parseLong(ranges[1])
            : fileSize - 1;

        // Apply max chunk size limit to prevent timeouts and connection resets
        // Mobile players will automatically request additional chunks as needed
        long maxChunkSize = streamingProperties.getMaxChunkSize();
        long end = Math.min(requestedEnd, start + maxChunkSize - 1);
        end = Math.min(end, fileSize - 1);

        // Validate range
        if (start < 0 || start >= fileSize || end < start) {
            throw new IllegalArgumentException(
                String.format("Invalid range: bytes=%d-%d (file size: %d)", start, end, fileSize));
        }

        return new RangeSpec(start, end);
    }

    /**
     * Build HTTP headers optimized for audio streaming.
     */
    private HttpHeaders buildStreamingHeaders(String contentType, boolean supportsRanges) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, contentType);

        if (supportsRanges) {
            headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");
        }

        // Cache audio files for 1 year (they are immutable)
        headers.add(HttpHeaders.CACHE_CONTROL,
            "public, max-age=" + streamingProperties.getCacheMaxAge());

        // Security headers
        headers.add("X-Content-Type-Options", "nosniff");

        return headers;
    }

    /**
     * Get streaming metadata without downloading the file.
     */
    public StreamingMetadata getStreamingMetadata(String objectKey) {
        var metadata = storageService.getObjectMetadata(objectKey);
        return new StreamingMetadata(
            objectKey,
            metadata.size(),
            metadata.contentType(),
            streamingProperties.getMaxChunkSize()
        );
    }

    /**
     * Record to hold range request specifications.
     */
    private record RangeSpec(long start, long end) {}

    /**
     * Record to hold streaming metadata.
     */
    public record StreamingMetadata(
        String objectKey,
        long fileSize,
        String contentType,
        long maxChunkSize
    ) {}
}
