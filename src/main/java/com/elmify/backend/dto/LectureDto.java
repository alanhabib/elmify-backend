package com.elmify.backend.dto;

import com.elmify.backend.entity.Lecture;
import com.elmify.backend.service.StorageService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import jakarta.validation.constraints.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;

// Using a Java Record for an immutable Data Transfer Object
@JsonAutoDetect(fieldVisibility = Visibility.ANY, creatorVisibility = Visibility.ANY)
public record LectureDto(
        Long id,

        @NotBlank(message = "Title is required")
        @Size(max = 500, message = "Title cannot exceed 500 characters")
        String title,

        // Keep all the direct properties of a lecture
        String genre,
        Integer year,

        @NotNull(message = "Duration is required")
        @PositiveOrZero(message = "Duration must be non-negative")
        Integer duration,

        String fileName,
        String filePath,

        @NotNull(message = "File size is required")
        @PositiveOrZero(message = "File size must be positive")
        Long fileSize,

        String fileFormat,
        String description,
        Integer lectureNumber,
        String thumbnailUrl,
        String audioUrl, // Full R2 public URL for direct streaming

        // Include IDs and important fields from related entities for the client
        Long speakerId,
        String speakerName,
        Long collectionId,
        String collectionTitle,

        // Timestamps
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    private static final Logger logger = LoggerFactory.getLogger(LectureDto.class);

    /**
     * A static factory method to create a DTO from a Lecture entity.
     * This is the primary way you'll convert your database objects for API responses.
     *
     * @param lecture The Lecture entity from the database.
     * @return A flattened LectureDto suitable for sending to a client (without presigned URLs).
     * @deprecated Use {@link #fromEntity(Lecture, StorageService)} to include presigned URLs for thumbnails.
     */
    public static LectureDto fromEntity(Lecture lecture) {
        // Use a null-safe way to get the related data
        Long speakerId = (lecture.getSpeaker() != null) ? lecture.getSpeaker().getId() : null;
        String speakerName = (lecture.getSpeaker() != null) ? lecture.getSpeaker().getName() : null;
        Long collectionId = (lecture.getCollection() != null) ? lecture.getCollection().getId() : null;
        String collectionTitle = (lecture.getCollection() != null) ? lecture.getCollection().getTitle() : null;

        return new LectureDto(
                lecture.getId(),
                lecture.getTitle(),
                lecture.getGenre(),
                lecture.getYear(),
                lecture.getDuration(),
                lecture.getFileName(),
                lecture.getFilePath(),
                lecture.getFileSize(),
                lecture.getFileFormat(),
                lecture.getDescription(),
                lecture.getLectureNumber(),
                lecture.getThumbnailUrl(),
                lecture.getAudioUrl(), // Return full R2 URL
                speakerId,
                speakerName,
                collectionId,
                collectionTitle,
                lecture.getCreatedAt(),
                lecture.getUpdatedAt()
        );
    }

    /**
     * Create a LectureDto from a Lecture entity with presigned URLs for thumbnails.
     * Converts relative MinIO paths to presigned URLs that can be used by clients.
     * If a lecture doesn't have its own thumbnail, it inherits the cover image from its parent collection.
     *
     * @param lecture The Lecture entity from the database.
     * @param storageService The storage service to generate presigned URLs.
     * @return A new LectureDto instance with presigned thumbnail URLs.
     */
    public static LectureDto fromEntity(Lecture lecture, StorageService storageService) {
        Long speakerId = (lecture.getSpeaker() != null) ? lecture.getSpeaker().getId() : null;
        String speakerName = (lecture.getSpeaker() != null) ? lecture.getSpeaker().getName() : null;
        Long collectionId = (lecture.getCollection() != null) ? lecture.getCollection().getId() : null;
        String collectionTitle = (lecture.getCollection() != null) ? lecture.getCollection().getTitle() : null;

        // Determine thumbnail URL: use lecture's own thumbnail, or fall back to collection's cover image
        String thumbnailPath = lecture.getThumbnailUrl();
        logger.info("Lecture {} - Original thumbnailUrl: '{}', Collection: {}",
                lecture.getId(), thumbnailPath, lecture.getCollection() != null ? lecture.getCollection().getId() : "null");

        if ((thumbnailPath == null || thumbnailPath.trim().isEmpty()) && lecture.getCollection() != null) {
            // Fallback to collection's cover image (prefer small version for performance)
            thumbnailPath = lecture.getCollection().getCoverImageSmallUrl();
            logger.info("Lecture {} - Trying collection.coverImageSmallUrl: '{}'", lecture.getId(), thumbnailPath);

            if (thumbnailPath == null || thumbnailPath.trim().isEmpty()) {
                thumbnailPath = lecture.getCollection().getCoverImageUrl();
                logger.info("Lecture {} - Trying collection.coverImageUrl: '{}'", lecture.getId(), thumbnailPath);
            }
            logger.info("Lecture {} - Final fallback path: '{}'", lecture.getId(), thumbnailPath);
        }

        return new LectureDto(
                lecture.getId(),
                lecture.getTitle(),
                lecture.getGenre(),
                lecture.getYear(),
                lecture.getDuration(),
                lecture.getFileName(),
                lecture.getFilePath(),
                lecture.getFileSize(),
                lecture.getFileFormat(),
                lecture.getDescription(),
                lecture.getLectureNumber(),
                convertToPresignedUrl(thumbnailPath, storageService),
                lecture.getAudioUrl(), // Return full R2 URL
                speakerId,
                speakerName,
                collectionId,
                collectionTitle,
                lecture.getCreatedAt(),
                lecture.getUpdatedAt()
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

    /**
     * Converts this DTO to a basic Lecture entity.
     * NOTE: This method does NOT handle setting the relationships (Speaker, Collection).
     * The service layer is responsible for fetching and setting the related entities
     * using the speakerId and collectionId from the DTO.
     *
     * @return A Lecture entity with primitive fields set.
     */
    public Lecture toEntity() {
        Lecture lecture = new Lecture();
        lecture.setId(this.id);
        lecture.setTitle(this.title);
        lecture.setGenre(this.genre);
        lecture.setYear(this.year);
        lecture.setDuration(this.duration);
        lecture.setFileName(this.fileName);
        lecture.setFilePath(this.filePath);
        lecture.setFileSize(this.fileSize);
        lecture.setFileFormat(this.fileFormat);
        lecture.setDescription(this.description);
        lecture.setLectureNumber(this.lectureNumber);
        lecture.setThumbnailUrl(this.thumbnailUrl);
        lecture.setAudioUrl(this.audioUrl);
        // Note: speaker and collection are intentionally left null here.
        return lecture;
    }
}