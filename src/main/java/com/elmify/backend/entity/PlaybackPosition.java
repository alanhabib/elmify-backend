package com.elmify.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "playback_positions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "lecture_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
public class PlaybackPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @Column(name = "current_position", nullable = false)
    private Integer currentPosition = 0;

    @LastModifiedDate
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    public PlaybackPosition(User user, Lecture lecture, Integer currentPosition) {
        this.user = user;
        this.lecture = lecture;
        this.currentPosition = currentPosition;
    }
}
