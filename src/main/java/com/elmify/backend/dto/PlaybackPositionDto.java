package com.elmify.backend.dto;

import com.elmify.backend.entity.PlaybackPosition;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.time.LocalDateTime;

/**
 * Playback Position DTO
 * Tracks where a user left off in a lecture
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record PlaybackPositionDto(
        String userId,
        Long lectureId,
        Integer currentPosition,
        LocalDateTime lastUpdated
) {
    public static PlaybackPositionDto fromEntity(PlaybackPosition position) {
        return new PlaybackPositionDto(
                position.getUser().getClerkId(),
                position.getLecture().getId(),
                position.getCurrentPosition(),
                position.getLastUpdated()
        );
    }
}
