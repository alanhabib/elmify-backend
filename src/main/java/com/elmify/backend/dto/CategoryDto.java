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
     * Create a CategoryDto from a Category entity, safely handling lazy-loaded parent.
     * This method uses getParentId() which only accesses the parent if already initialized,
     * preventing LazyInitializationException. Best used for subcategories where parent
     * relationship may not be eagerly fetched.
     *
     * @param category The Category entity from the database.
     * @return A new CategoryDto instance. The parentId will be null if parent is not initialized.
     */
    public static CategoryDto fromEntityWithoutParent(Category category) {
        return new CategoryDto(
            category.getId(),
            category.getName(),
            category.getSlug(),
            category.getDescription(),
            category.getIconName(),
            category.getColor(),
            category.getParentId(), // Returns null if parent is not initialized
            category.getLectureCount(),
            category.getCollectionCount(),
            category.getIsFeatured()
        );
    }
}
