package com.audibleclone.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Speaker entity representing audio content speakers/instructors.
 */
@Entity
@Table(name = "speakers")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
public class Speaker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "image_small_url")
    private String imageSmallUrl;

    @NotNull
    @Column(name = "is_premium", nullable = false)
    private Boolean isPremium = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- Relationships ---
    @OneToMany(mappedBy = "speaker", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude // Prevent circular toString calls and lazy loading issues
    private List<Collection> collections = new ArrayList<>();

    // --- Constructors ---
    public Speaker(String name) {
        this.name = name;
    }
}