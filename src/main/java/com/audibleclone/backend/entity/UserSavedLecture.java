package com.audibleclone.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "user_saved_lectures")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(UserSavedLectureId.class) // Specify the ID class
public class UserSavedLecture {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id")
    private Lecture lecture;

    // Note: The 'created_at' column in your schema is an integer epoch.
    // This is unusual but can be handled. A standard timestamp is often easier.
    @Column(name = "created_at", nullable = false)
    private long createdAt;
}
