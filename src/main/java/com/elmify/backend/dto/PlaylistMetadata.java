package com.elmify.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Playlist metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistMetadata implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Total number of tracks in playlist
     */
    private Integer totalTracks;

    /**
     * Total duration of all tracks in seconds
     */
    private Long totalDuration;

    /**
     * Timestamp when manifest was generated
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant generatedAt;

    /**
     * When the URLs in this manifest expire
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant expiresAt;

    /**
     * Whether this response was served from cache
     */
    private Boolean cached;
}
