package com.elmify.backend.dto;

import com.elmify.backend.entity.LectureCategory;

/**
 * Minimal Category DTO for embedding in LectureDto.
 * Contains only essential fields needed for display.
 */
public record CategorySummaryDto(
    Long id,
    String name,
    String slug,
    Boolean isPrimary
) {
    /**
     * Create a CategorySummaryDto from a LectureCategory entity.
     *
     * @param lectureCategory The LectureCategory junction entity.
     * @return A new CategorySummaryDto instance.
     */
    public static CategorySummaryDto fromLectureCategory(LectureCategory lectureCategory) {
        return new CategorySummaryDto(
            lectureCategory.getCategory().getId(),
            lectureCategory.getCategory().getName(),
            lectureCategory.getCategory().getSlug(),
            lectureCategory.getIsPrimary()
        );
    }
}
