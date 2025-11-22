package com.elmify.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "lecture_categories")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
public class LectureCategory {

    @EmbeddedId
    private LectureCategoryId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("lectureId")
    @JoinColumn(name = "lecture_id")
    @ToString.Exclude
    private Lecture lecture;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("categoryId")
    @JoinColumn(name = "category_id")
    @ToString.Exclude
    private Category category;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public LectureCategory(Lecture lecture, Category category, Boolean isPrimary) {
        this.id = new LectureCategoryId(lecture.getId(), category.getId());
        this.lecture = lecture;
        this.category = category;
        this.isPrimary = isPrimary;
    }
}
