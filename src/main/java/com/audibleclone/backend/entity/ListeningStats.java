package com.audibleclone.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "listening_stats", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "lecture_id", "date"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListeningStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "total_play_time")
    private Integer totalPlayTime = 0;

    @Column(name = "play_count")
    private Integer playCount = 0;

    @Column(name = "completion_rate")
    private Float completionRate = 0.0f;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
