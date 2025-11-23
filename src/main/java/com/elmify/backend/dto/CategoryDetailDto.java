package com.elmify.backend.dto;

import com.elmify.backend.entity.Category;
import com.elmify.backend.entity.Collection;
import com.elmify.backend.service.StorageService;

import java.util.List;

/**
 * Extended Category DTO with subcategories and featured collections.
 * Used for category detail views.
 */
public record CategoryDetailDto(
    Long id,
    String name,
    String slug,
    String description,
    String iconName,
    String color,
    Long parentId,
    Integer lectureCount,
    Integer collectionCount,
    Boolean isFeatured,
    List<CategoryDto> subcategories,
    List<CollectionDto> featuredCollections
) {
    /**
     * Create a CategoryDetailDto from a Category entity with related data.
     *
     * @param category The Category entity.
     * @param subcategories List of subcategories.
     * @param featuredCollections List of featured collections in this category.
     * @param storageService Storage service for presigned URLs.
     * @return A new CategoryDetailDto instance.
     */
    public static CategoryDetailDto fromEntity(
        Category category,
        List<Category> subcategories,
        List<Collection> featuredCollections,
        StorageService storageService
    ) {
        return new CategoryDetailDto(
            category.getId(),
            category.getName(),
            category.getSlug(),
            category.getDescription(),
            category.getIconName(),
            category.getColor(),
            category.getParent() != null ? category.getParent().getId() : null,
            category.getLectureCount(),
            category.getCollectionCount(),
            category.getIsFeatured(),
            subcategories.stream().map(CategoryDto::fromEntityWithoutParent).toList(),
            featuredCollections.stream()
                .map(c -> CollectionDto.fromEntity(c, storageService))
                .toList()
        );
    }
}
