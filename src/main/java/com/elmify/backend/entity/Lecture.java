package com.elmify.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "lectures")
@EntityListeners(AuditingEntityListener.class) // Enables @CreatedDate and @LastModifiedDate
@Data // Lombok: Generates getters, setters, toString, equals, and hashCode
@NoArgsConstructor // Lombok: Generates a no-argument constructor
public class Lecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include // Ensures Lombok's equals/hashCode only uses the ID
    @ToString.Include // Ensures Lombok's toString only includes the ID
    private Long id;

    @Column(name = "directory_id")
    private Integer directoryId;

    @NotBlank
    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "genre")
    private String genre;

    @Column(name = "year")
    private Integer year;

    @NotNull
    @PositiveOrZero
    @Column(name = "duration", nullable = false)
    private Integer duration; // Duration in seconds

    @NotBlank
    @Column(name = "file_name", nullable = false)
    private String fileName;

    @NotBlank
    @Column(name = "file_path", nullable = false)
    private String filePath;

    @NotNull
    @PositiveOrZero
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @NotBlank
    @Column(name = "file_format", nullable = false)
    private String fileFormat;

    @Column(name = "bitrate")
    private Integer bitrate;

    @Column(name = "sample_rate")
    private Integer sampleRate;

    @Column(name = "file_hash")
    private String fileHash; // e.g., SHA-256 hash

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "audio_url", length = 1024)
    private String audioUrl; // Full public R2 URL for direct streaming

    @Column(name = "waveform_data", columnDefinition = "TEXT")
    private String waveformData; // JSON waveform data

    @NotNull
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    @NotNull
    @PositiveOrZero
    @Column(name = "play_count", nullable = false)
    private Integer playCount = 0;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "lecture_number")
    private Integer lectureNumber;

    // --- Relationships ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "speaker_id")
    @ToString.Exclude // Avoid circular dependencies in toString
    private Speaker speaker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    @ToString.Exclude // Avoid circular dependencies in toString
    private Collection collection;

    // --- Timestamps ---
    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "last_played_at")
    private LocalDateTime lastPlayedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- Constructors ---
    public Lecture(String title, String filePath, Long fileSize, String fileFormat, Integer duration) {
        this.title = title;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.fileFormat = fileFormat;
        this.duration = duration;
    }

    public String getFormattedDuration() {
        if (duration == null || duration <= 0) {
            return "Unknown Duration";
        }

        if (duration < 60) {
            return duration + " seconds";
        } else if (duration < 3600) {
            return (duration / 60) + " minutes";
        } else {
            return (duration / 3600) + " hours, " + ((duration % 3600) / 60) + " minutes";
        }
    }

    public boolean isAudioFile() {
        if (fileFormat == null) return false;
        switch (fileFormat.toLowerCase()) {
            case "mp3":
            case "wav":
            case "flac":
            case "aac":
            case "ogg":
            case "m4a":
                return true;
            default:
                return false;
        }
    }

    // Helper for file size formatting
    public String getFormattedSize() {
        if (fileSize == null) {
            return "Unknown Size";
        }

        if (fileSize < 1024) {
            return fileSize + " bytes";
        } else if (fileSize < 1024 * 1024) {
            return (fileSize / 1024) + " KB";
        } else if (fileSize < 1024L * 1024 * 1024) {
            return (fileSize / (1024 * 1024)) + " MB";
        } else {
            return (fileSize / (1024L * 1024 * 1024)) + " GB";
        }
    }

    /**
     * Check if this lecture is premium by checking if its speaker is premium.
     * Lectures inherit premium status from their speaker.
     *
     * @return true if the speaker is premium, false otherwise
     */
    public boolean isPremium() {
        return speaker != null && speaker.getIsPremium() != null && speaker.getIsPremium();
    }
}
