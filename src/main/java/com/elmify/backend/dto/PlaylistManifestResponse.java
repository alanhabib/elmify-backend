package com.elmify.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Response DTO for playlist manifest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistManifestResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Collection ID or playlist type identifier
     */
    private String collectionId;

    /**
     * Array of track manifests with pre-signed URLs
     */
    private List<TrackManifest> tracks;

    /**
     * Playlist metadata
     */
    private PlaylistMetadata metadata;
}
