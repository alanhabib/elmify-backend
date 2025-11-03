package com.audibleclone.backend.dto;

import com.audibleclone.backend.entity.Collection;
import com.audibleclone.backend.service.StorageService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * A Data Transfer Object representing a Collection.
 * This is used for API responses and as an input model for creating/updating collections.
 *
 * @param id The unique ID of the collection.
 * @param title The title of the collection.
 * @param year The year the collection was released.
 * @param coverImageUrl URL for the main cover image.
 * @param coverImageSmallUrl URL for the small/thumbnail cover image.
 * @param speakerId The ID of the associated speaker. Required for creation.
 * @param speakerName The name of the associated speaker.
 * @param lectureCount The total number of lectures in the collection (calculated by the database).
 * @param createdAt The timestamp when the collection was created.
 * @param updatedAt The timestamp when the collection was last updated.
 */
public record CollectionDto(
        Long id,

        @NotBlank(message = "Title is required")
        @Size(max = 500, message = "Title cannot exceed 500 characters")
        String title,

        @Min(value = 1000, message = "Year must be a valid 4-digit year")
        @Max(value = 9999, message = "Year must be a valid 4-digit year")
        Integer year,

        String coverImageUrl,
        String coverImageSmallUrl,

        @NotNull(message = "A speaker ID is required to create or update a collection")
        Long speakerId,

        // Fields included in responses but not used for creation/update
        String speakerName,
        int lectureCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    private static final Logger logger = LoggerFactory.getLogger(CollectionDto.class);

    /**
     * A static factory method to create a CollectionDto from a Collection entity.
     * This is the standard way to convert database objects for API responses.
     *
     * @param collection The Collection entity from the database.
     * @return A new CollectionDto instance (without presigned URLs).
     * @deprecated Use {@link #fromEntity(Collection, StorageService)} to include presigned URLs for images.
     */
    public static CollectionDto fromEntity(Collection collection) {
        Long speakerId = (collection.getSpeaker() != null) ? collection.getSpeaker().getId() : null;
        String speakerName = (collection.getSpeaker() != null) ? collection.getSpeaker().getName() : null;

        return new CollectionDto(
                collection.getId(),
                collection.getTitle(),
                collection.getYear(),
                collection.getCoverImageUrl(),
                collection.getCoverImageSmallUrl(),
                speakerId,
                speakerName,
                collection.getLectureCount(),
                collection.getCreatedAt(),
                collection.getUpdatedAt()
        );
    }

    /**
     * Create a CollectionDto from a Collection entity with presigned URLs for images.
     * Converts relative MinIO paths to presigned URLs that can be used by clients.
     *
     * @param collection The Collection entity from the database.
     * @param storageService The storage service to generate presigned URLs.
     * @return A new CollectionDto instance with presigned image URLs.
     */
    public static CollectionDto fromEntity(Collection collection, StorageService storageService) {
        Long speakerId = (collection.getSpeaker() != null) ? collection.getSpeaker().getId() : null;
        String speakerName = (collection.getSpeaker() != null) ? collection.getSpeaker().getName() : null;

        return new CollectionDto(
                collection.getId(),
                collection.getTitle(),
                collection.getYear(),
                convertToPresignedUrl(collection.getCoverImageUrl(), storageService),
                convertToPresignedUrl(collection.getCoverImageSmallUrl(), storageService),
                speakerId,
                speakerName,
                collection.getLectureCount(),
                collection.getCreatedAt(),
                collection.getUpdatedAt()
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

    // A toEntity() method is intentionally omitted.
    // The conversion from DTO to a full Entity (with relationships) is the responsibility
    // of the Service layer, which can use the speakerId to fetch the Speaker entity.
    // This maintains a clean separation of concerns.
}