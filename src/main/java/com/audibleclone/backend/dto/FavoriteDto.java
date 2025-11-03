package com.audibleclone.backend.dto;

import com.audibleclone.backend.entity.Favorite;
import com.audibleclone.backend.service.StorageService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.time.LocalDateTime;

/**
 * DTO for Favorite entity
 * Includes the full lecture information for the favorited lecture
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record FavoriteDto(
        Long id,
        String userId,
        LectureDto lecture,
        LocalDateTime createdAt
) {

    /**
     * Convert a Favorite entity to a DTO with lecture details
     */
    public static FavoriteDto fromEntity(Favorite favorite, StorageService storageService) {
        return new FavoriteDto(
                favorite.getId(),
                favorite.getUser().getClerkId(),
                LectureDto.fromEntity(favorite.getLecture(), storageService),
                favorite.getCreatedAt()
        );
    }
}
