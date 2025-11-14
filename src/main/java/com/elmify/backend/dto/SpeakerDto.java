package com.elmify.backend.dto;

import com.elmify.backend.entity.Speaker;
import com.elmify.backend.service.StorageService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * A Data Transfer Object representing a Speaker.
 * This is used for API responses and as an input model for creating/updating speakers.
 *
 * @param id The unique ID of the speaker.
 * @param name The name of the speaker.
 * @param bio The biography or description of the speaker.
 * @param imageUrl URL for the main speaker image.
 * @param imageSmallUrl URL for the small/thumbnail speaker image.
 * @param isPremium The premium status of the speaker.
 * @param createdAt The timestamp when the speaker was created.
 * @param updatedAt The timestamp when the speaker was last updated.
 */
public record SpeakerDto(
        Long id,

        @NotBlank(message = "Speaker name is required")
        @Size(max = 255)
        String name,

        String bio,
        String imageUrl,
        String imageSmallUrl,
        boolean isPremium,

        // Fields for API response, not for creation/update input
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    private static final Logger logger = LoggerFactory.getLogger(SpeakerDto.class);

    /**
     * A static factory method to create a SpeakerDto from a Speaker entity.
     * This is the standard way to convert database objects for API responses.
     *
     * @param speaker The Speaker entity from the database.
     * @return A new SpeakerDto instance (without presigned URLs).
     * @deprecated Use {@link #fromEntity(Speaker, StorageService)} to include presigned URLs for images.
     */
    public static SpeakerDto fromEntity(Speaker speaker) {
        return new SpeakerDto(
                speaker.getId(),
                speaker.getName(),
                speaker.getBio(),
                speaker.getImageUrl(),
                speaker.getImageSmallUrl(),
                speaker.getIsPremium(),
                speaker.getCreatedAt(),
                speaker.getUpdatedAt()
        );
    }

    /**
     * Create a SpeakerDto from a Speaker entity with presigned URLs for images.
     * Converts relative MinIO paths to presigned URLs that can be used by clients.
     *
     * @param speaker The Speaker entity from the database.
     * @param storageService The storage service to generate presigned URLs.
     * @return A new SpeakerDto instance with presigned image URLs.
     */
    public static SpeakerDto fromEntity(Speaker speaker, StorageService storageService) {
        return new SpeakerDto(
                speaker.getId(),
                speaker.getName(),
                speaker.getBio(),
                convertToPresignedUrl(speaker.getImageUrl(), storageService),
                convertToPresignedUrl(speaker.getImageSmallUrl(), storageService),
                speaker.getIsPremium(),
                speaker.getCreatedAt(),
                speaker.getUpdatedAt()
        );
    }

    /**
     * Convert a relative MinIO path to a presigned URL.
     * If the path is already a full URL, returns it as-is.
     * If the path is null or empty, returns null.
     *
     * @param path The relative path or existing URL.
     * @param storageService The storage service to generate presigned URLs.
     * @return A presigned URL or the original URL, or null if path is invalid.
     */
    private static String convertToPresignedUrl(String path, StorageService storageService) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        // If already a full URL, return as-is
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }

        // Convert relative path to presigned URL
        try {
            String presignedUrl = storageService.generatePresignedUrl(path);
            logger.debug("Converted path '{}' to presigned URL", path);
            return presignedUrl;
        } catch (Exception e) {
            logger.warn("Failed to generate presigned URL for path '{}': {}", path, e.getMessage());
            return null;
        }
    }
}