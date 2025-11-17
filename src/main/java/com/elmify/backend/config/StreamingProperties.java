package com.elmify.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Configuration properties for audio streaming.
 * Allows customization of streaming behavior for different environments.
 */
@Configuration
@ConfigurationProperties(prefix = "elmify.streaming")
@Validated
public class StreamingProperties {

    /**
     * Maximum chunk size for range requests (in bytes).
     * Default: 10MB - optimal for mobile networks and prevents connection timeouts
     */
    @NotNull
    @Min(1024 * 1024) // Minimum 1MB
    private Long maxChunkSize = 10 * 1024 * 1024L; // 10MB default

    /**
     * Buffer size for streaming (in bytes).
     * Default: 8KB - standard for efficient I/O operations
     */
    @NotNull
    @Min(1024) // Minimum 1KB
    private Integer bufferSize = 8192; // 8KB default

    /**
     * Enable gzip compression for audio streams.
     * Default: false - audio files are already compressed
     */
    private Boolean enableCompression = false;

    /**
     * Cache control max-age in seconds.
     * Default: 1 year (31536000 seconds) for immutable audio files
     */
    @NotNull
    @Min(0)
    private Long cacheMaxAge = 31536000L; // 1 year

    /**
     * Enable detailed logging for streaming operations.
     * Useful for debugging but may impact performance.
     */
    private Boolean enableDetailedLogging = false;

    // Getters and Setters

    public Long getMaxChunkSize() {
        return maxChunkSize;
    }

    public void setMaxChunkSize(Long maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }

    public Integer getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(Integer bufferSize) {
        this.bufferSize = bufferSize;
    }

    public Boolean getEnableCompression() {
        return enableCompression;
    }

    public void setEnableCompression(Boolean enableCompression) {
        this.enableCompression = enableCompression;
    }

    public Long getCacheMaxAge() {
        return cacheMaxAge;
    }

    public void setCacheMaxAge(Long cacheMaxAge) {
        this.cacheMaxAge = cacheMaxAge;
    }

    public Boolean getEnableDetailedLogging() {
        return enableDetailedLogging;
    }

    public void setEnableDetailedLogging(Boolean enableDetailedLogging) {
        this.enableDetailedLogging = enableDetailedLogging;
    }
}
