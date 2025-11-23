package com.elmify.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LectureCategoryId implements Serializable {

    @Column(name = "lecture_id")
    private Long lectureId;

    @Column(name = "category_id")
    private Long categoryId;
}
