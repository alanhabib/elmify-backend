package com.elmify.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Track manifest with pre-signed URL
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackManifest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique lecture identifier
     */
    private String lectureId;

    /**
     * Pre-signed R2 URL for audio streaming
     * Valid for 4 hours from generation
     */
    private String audioUrl;

    /**
     * URL expiration timestamp
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant expiresAt;

    /**
     * Audio duration in seconds
     */
    private Long duration;
}
