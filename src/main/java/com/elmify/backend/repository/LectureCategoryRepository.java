package com.elmify.backend.repository;

import com.elmify.backend.entity.LectureCategory;
import com.elmify.backend.entity.LectureCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LectureCategoryRepository extends JpaRepository<LectureCategory, LectureCategoryId> {

    /**
     * Find all category associations for a lecture.
     */
    @Query("SELECT lc FROM LectureCategory lc JOIN FETCH lc.category WHERE lc.lecture.id = :lectureId")
    List<LectureCategory> findByLectureIdWithCategory(@Param("lectureId") Long lectureId);

    /**
     * Find all lecture associations for a category.
     */
    @Query("SELECT lc FROM LectureCategory lc WHERE lc.category.id = :categoryId")
    List<LectureCategory> findByCategoryId(@Param("categoryId") Long categoryId);
}
