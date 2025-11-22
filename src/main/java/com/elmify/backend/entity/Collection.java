package com.elmify.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Formula;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Collection entity representing a collection of lectures by a speaker.
 */
@Entity
@Table(name = "collections",
        uniqueConstraints = @UniqueConstraint(columnNames = {"speaker_id", "title"}))
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
public class Collection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "speaker_id", nullable = false)
    @ToString.Exclude // Prevent circular toString calls
    private Speaker speaker;

    @NotBlank
    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "year")
    private Integer year;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "cover_image_small_url")
    private String coverImageSmallUrl;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- Relationships ---
    @OneToMany(mappedBy = "collection", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude // Avoid fetching a potentially large list for toString
    private List<Lecture> lectures = new ArrayList<>();

    @OneToMany(mappedBy = "collection", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private Set<CollectionCategory> collectionCategories = new HashSet<>();

    // --- Calculated Fields ---
    // This is a great performance optimization. It calculates the count in the DB.
    @Formula("(SELECT COUNT(*) FROM lectures l WHERE l.collection_id = id)")
    private int lectureCount;

    // --- Constructors ---
    public Collection(Speaker speaker, String title) {
        this.speaker = speaker;
        this.title = title;
    }

    public Collection(Speaker speaker, String title, Integer year) {
        this.speaker = speaker;
        this.title = title;
        this.year = year;
    }

    // --- Helper Methods ---
    // The @Formula field provides the count directly, no helper method needed.

    public String getSpeakerName() {
        return speaker != null ? speaker.getName() : null;
    }

    /**
     * Check if this collection is premium by checking if its speaker is premium.
     * Collections inherit premium status from their speaker.
     *
     * @return true if the speaker is premium, false otherwise
     */
    public boolean isPremium() {
        return speaker != null && speaker.getIsPremium() != null && speaker.getIsPremium();
    }
}