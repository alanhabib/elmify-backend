package com.elmify.backend.dto;

import com.elmify.backend.entity.Category;

/**
 * Data Transfer Object for Category.
 * Used for list views and embedded category references.
 */
public record CategoryDto(
    Long id,
    String name,
    String slug,
    String description,
    String iconName,
    String color,
    Long parentId,
    Integer lectureCount,
    Integer collectionCount,
    Boolean isFeatured
) {
    /**
     * Create a CategoryDto from a Category entity.
     *
     * @param category The Category entity from the database.
     * @return A new CategoryDto instance.
     */
    public static CategoryDto fromEntity(Category category) {
        return new CategoryDto(
            category.getId(),
            category.getName(),
            category.getSlug(),
            category.getDescription(),
            category.getIconName(),
            category.getColor(),
            category.getParentId(),
            category.getLectureCount(),
            category.getCollectionCount(),
            category.getIsFeatured()
        );
    }

    /**
     * Create a CategoryDto from a Category entity without accessing the parent.
     * Used for subcategories where parent relationship is not needed.
     *
     * @param category The Category entity from the database.
     * @return A new CategoryDto instance with null parentId.
     */
    public static CategoryDto fromEntityWithoutParent(Category category) {
        return new CategoryDto(
            category.getId(),
            category.getName(),
            category.getSlug(),
            category.getDescription(),
            category.getIconName(),
            category.getColor(),
            null, // Don't access parent to avoid lazy loading
            category.getLectureCount(),
            category.getCollectionCount(),
            category.getIsFeatured()
        );
    }
}
