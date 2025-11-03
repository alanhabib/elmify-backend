package com.elmify.backend.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * A mobile-optimized wrapper for paginated API responses.
 * Transforms Spring's Page object into a clean, predictable format for mobile apps.
 *
 * Benefits:
 * - Clean field names (data, currentPage vs content, number)
 * - Only essential pagination metadata
 * - Removes Spring Boot internals (pageable, sort, etc.)
 * - Consistent across all paginated endpoints
 *
 * @param <T> The type of items in the page (e.g., SpeakerDto, CollectionDto)
 */
public record PagedResponse<T>(
        List<T> data,
        PaginationMeta pagination
) {
    /**
     * Pagination metadata for mobile apps
     */
    public record PaginationMeta(
            int currentPage,    // Current page number (0-indexed)
            int pageSize,       // Number of items per page
            long totalItems,    // Total number of items across all pages
            int totalPages,     // Total number of pages
            boolean hasNext,    // True if there's a next page
            boolean hasPrevious // True if there's a previous page
    ) {}

    /**
     * Factory method to create PagedResponse from Spring's Page object
     *
     * @param page Spring Data Page object
     * @param <T> Type of content
     * @return Mobile-optimized PagedResponse
     */
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                new PaginationMeta(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages(),
                        page.hasNext(),
                        page.hasPrevious()
                )
        );
    }

    /**
     * Convenience factory for empty results
     *
     * @param <T> Type of content
     * @return Empty PagedResponse
     */
    public static <T> PagedResponse<T> empty() {
        return new PagedResponse<>(
                List.of(),
                new PaginationMeta(0, 0, 0, 0, false, false)
        );
    }
}
