package com.elmify.backend.controller;

import com.elmify.backend.dto.CategoryDetailDto;
import com.elmify.backend.dto.CategoryDto;
import com.elmify.backend.dto.LectureDto;
import com.elmify.backend.dto.PagedResponse;
import com.elmify.backend.entity.Category;
import com.elmify.backend.entity.Collection;
import com.elmify.backend.exception.ResourceNotFoundException;
import com.elmify.backend.service.CategoryService;
import com.elmify.backend.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for browsing categories and category content.
 */
@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Endpoints for browsing categories and category content")
@RequiredArgsConstructor
@Validated
public class CategoryController {

    private final CategoryService categoryService;
    private final StorageService storageService;

    @GetMapping
    @Operation(summary = "Get All Top-Level Categories",
            description = "Retrieves all active top-level categories (no parent). Public endpoint.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved categories")
    public ResponseEntity<List<CategoryDto>> getAllCategories() {
        List<Category> categories = categoryService.getAllTopLevelCategories();
        List<CategoryDto> categoryDtos = categories.stream()
                .map(CategoryDto::fromEntity)
                .toList();
        return ResponseEntity.ok(categoryDtos);
    }

    @GetMapping("/featured")
    @Operation(summary = "Get Featured Categories",
            description = "Retrieves all featured categories for homepage display. Public endpoint.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved featured categories")
    public ResponseEntity<List<CategoryDto>> getFeaturedCategories() {
        List<Category> categories = categoryService.getFeaturedCategories();
        List<CategoryDto> categoryDtos = categories.stream()
                .map(CategoryDto::fromEntity)
                .toList();
        return ResponseEntity.ok(categoryDtos);
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get Category by Slug",
            description = "Retrieves a category with its subcategories and featured collections. Public endpoint.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved category")
    @ApiResponse(responseCode = "404", description = "Category not found")
    public ResponseEntity<CategoryDetailDto> getCategoryBySlug(
            @Parameter(description = "Category slug", example = "quran-tafsir")
            @PathVariable String slug) {
        Category category = categoryService.getCategoryBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));

        List<Category> subcategories = categoryService.getSubcategories(category.getId());
        List<Collection> featuredCollections = categoryService.getFeaturedCollectionsForCategory(slug);

        CategoryDetailDto dto = CategoryDetailDto.fromEntity(
                category,
                subcategories,
                featuredCollections,
                storageService
        );

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{slug}/subcategories")
    @Operation(summary = "Get Subcategories",
            description = "Retrieves all active subcategories of a category. Public endpoint.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved subcategories")
    @ApiResponse(responseCode = "404", description = "Category not found")
    public ResponseEntity<List<CategoryDto>> getSubcategories(
            @Parameter(description = "Parent category slug", example = "quran")
            @PathVariable String slug) {
        Category parent = categoryService.getCategoryBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));

        List<Category> subcategories = categoryService.getSubcategories(parent.getId());
        List<CategoryDto> categoryDtos = subcategories.stream()
                .map(CategoryDto::fromEntity)
                .toList();

        return ResponseEntity.ok(categoryDtos);
    }

    @GetMapping("/{slug}/lectures")
    @Operation(summary = "Get Lectures by Category (Paginated)",
            description = "Retrieves paginated lectures in a category. Filters premium content for non-premium users. Public endpoint.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved lectures")
    @ApiResponse(responseCode = "404", description = "Category not found")
    public ResponseEntity<PagedResponse<LectureDto>> getLecturesByCategory(
            @Parameter(description = "Category slug", example = "quran")
            @PathVariable String slug,
            Pageable pageable) {
        // Verify category exists
        categoryService.getCategoryBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));

        Page<LectureDto> lectureDtos = categoryService.getLecturesByCategory(slug, pageable)
                .map(lecture -> LectureDto.fromEntity(lecture, storageService));

        PagedResponse<LectureDto> response = PagedResponse.from(lectureDtos);
        return ResponseEntity.ok(response);
    }
}
