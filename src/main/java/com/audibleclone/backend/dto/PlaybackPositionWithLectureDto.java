package com.audibleclone.backend.dto;

import com.audibleclone.backend.entity.PlaybackPosition;
import com.audibleclone.backend.service.StorageService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.time.LocalDateTime;

/**
 * Enhanced Playback Position DTO with Full Lecture Details
 * Used for endpoints that need to return playback positions along with lecture information
 * Primarily used for "Recent Lectures" and "Continue Listening" features
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record PlaybackPositionWithLectureDto(
        String userId,
        Long lectureId,
        Integer currentPosition,
        LocalDateTime lastUpdated,
        LectureDto lecture,
        Double progress  // Calculated progress percentage (0-100)
) {
    /**
     * Create from PlaybackPosition entity with lecture details
     * Includes automatic progress calculation
     */
    public static PlaybackPositionWithLectureDto fromEntity(PlaybackPosition position, StorageService storageService) {
        LectureDto lectureDto = LectureDto.fromEntity(position.getLecture(), storageService);

        // Calculate progress percentage
        Double progress = calculateProgress(position.getCurrentPosition(), position.getLecture().getDuration());

        return new PlaybackPositionWithLectureDto(
                position.getUser().getClerkId(),
                position.getLecture().getId(),
                position.getCurrentPosition(),
                position.getLastUpdated(),
                lectureDto,
                progress
        );
    }

    /**
     * Calculate progress percentage from current position and duration
     * Returns value between 0 and 100
     */
    private static Double calculateProgress(Integer currentPosition, Integer duration) {
        if (duration == null || duration <= 0 || currentPosition == null) {
            return 0.0;
        }

        double progress = ((double) currentPosition / duration) * 100.0;

        // Clamp between 0 and 100
        return Math.min(Math.max(progress, 0.0), 100.0);
    }
}
