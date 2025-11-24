package com.elmify.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for playlist manifest generation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistManifestRequest {

    /**
     * Collection ID for collection-based playlists
     */
    private String collectionId;

    /**
     * Type of dynamic playlist (alternative to collectionId)
     * Valid values: "favorites", "downloads", "history"
     */
    private String playlistType;

    /**
     * Array of lecture IDs to include in manifest
     * Order is preserved in the response
     */
    @NotNull(message = "lectureIds cannot be null")
    @NotEmpty(message = "lectureIds cannot be empty")
    @Size(min = 1, max = 1000, message = "lectureIds must contain between 1 and 1000 items")
    private List<String> lectureIds;
}
