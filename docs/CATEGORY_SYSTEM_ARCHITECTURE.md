# Elmify Category System Architecture

A comprehensive technical and UX documentation for implementing a full category system in the Elmify React Native Expo application.

---

## Table of Contents

1. [Design Principles & Best Practices](#1-design-principles--best-practices)
2. [Existing Codebase Integration](#2-existing-codebase-integration)
3. [Category System Architecture](#3-category-system-architecture)
4. [Backend Implementation](#4-backend-implementation)
5. [Frontend Implementation](#5-frontend-implementation)
6. [UI/UX Design](#6-uiux-design)
7. [Implementation Plan](#7-implementation-plan)
8. [Additional Improvements](#8-additional-improvements)

---

## 1. Design Principles & Best Practices

### 1.1 SOLID Principles Application

| Principle | Application in Category System |
|-----------|-------------------------------|
| **Single Responsibility** | `CategoryService` handles only category business logic; `CategoryController` handles only HTTP concerns |
| **Open/Closed** | Categories are extensible (add new ones) without modifying existing code |
| **Liskov Substitution** | DTOs can be extended for specialized views (e.g., `CategoryDetailDto extends CategoryDto`) |
| **Interface Segregation** | Separate interfaces for read operations vs write operations |
| **Dependency Inversion** | Services depend on repository interfaces, not implementations |

### 1.2 DRY (Don't Repeat Yourself)

**Reuse existing patterns:**
- Use existing `PagedResponse<T>` for pagination (don't create new wrapper)
- Use existing `PremiumFilterService` for access control
- Follow existing DTO record pattern with `fromEntity()` factory
- Extend existing query keys factory pattern

**Avoid redundancy:**
- Don't duplicate speaker/collection filtering logic
- Reuse existing `LectureDto` structure, just add categories field
- Use existing API client error handling

### 1.3 Database Design Principles

1. **Normalization**: Categories in separate table with junction tables (not JSON array)
2. **Referential Integrity**: Foreign keys with proper CASCADE rules
3. **Denormalization for Performance**: Store `lecture_count` on category (updated via triggers)
4. **Soft Deletes**: Use `is_active` flag, never hard delete categories
5. **Audit Trail**: Include `created_at`, `updated_at` timestamps

### 1.4 API Design Principles

1. **RESTful**: Resource-based URLs (`/categories/{id}/lectures`)
2. **Consistent Response Format**: Always use `PagedResponse<T>` for lists
3. **HATEOAS-lite**: Include related resource counts, not full nested objects
4. **Idempotent**: GET requests are safe to retry
5. **Versioned**: `/api/v1/categories`

### 1.5 Caching Strategy Principles

1. **Cache Invalidation**: Clear on writes, not time-based for critical data
2. **Cache Hierarchy**: Client (React Query) → Server (Redis) → Database
3. **Stale-While-Revalidate**: Show cached data while fetching fresh
4. **Granular Keys**: Cache by specific query params, not globally

---

## 2. Existing Codebase Integration

### 2.1 Current Entity Structure

```
Speaker (isPremium: boolean)
  └── Collection
        └── Lecture (genre: String) ← EXISTING FIELD TO ADDRESS
```

### 2.2 Decision: `genre` Field Migration

The `Lecture` entity currently has a `genre` field (String). Options:

**Recommended: Migrate and Deprecate**
1. Create new `Category` entity with normalized structure
2. Migrate existing `genre` values to categories
3. Mark `genre` field as `@Deprecated`
4. Remove in future version after full migration

```java
// Lecture.java - transition period
@Deprecated
@Column(name = "genre")
private String genre; // Will be removed in v2.0

@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
    name = "lecture_categories",
    joinColumns = @JoinColumn(name = "lecture_id"),
    inverseJoinColumns = @JoinColumn(name = "category_id")
)
private Set<Category> categories = new HashSet<>();
```

### 2.3 Integration Points

| Existing Component | Integration Approach |
|-------------------|---------------------|
| `PremiumFilterService` | Add `filterCategoriesByPremium()` method |
| `LectureDto` | Add `List<CategorySummaryDto> categories` field |
| `LectureController` | Add `?category={slug}` query parameter |
| `SearchService` | Add category faceting to search results |
| Browse tab (frontend) | Add categories section to existing screen |

### 2.4 Following Existing Patterns

**Backend patterns to follow:**
```java
// DTO as record (like existing LectureDto)
public record CategoryDto(
    Long id,
    String name,
    String slug,
    // ...
) {
    public static CategoryDto fromEntity(Category category) {
        return new CategoryDto(
            category.getId(),
            category.getName(),
            category.getSlug(),
            // ...
        );
    }
}

// Service pattern (like existing LectureService)
@Service
@Transactional(readOnly = true)
public class CategoryService {
    // ...
}

// Repository pattern (like existing LectureRepository)
public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.subcategories WHERE c.isActive = true")
    List<Category> findAllActive();
}
```

**Frontend patterns to follow:**
```typescript
// Query keys (following keys.ts pattern)
categories: {
  all: ['categories'] as const,
  list: () => [...queryKeys.categories.all, 'list'] as const,
  detail: (slug: string) => [...queryKeys.categories.all, 'detail', slug] as const,
  lectures: (slug: string) => [...queryKeys.categories.all, slug, 'lectures'] as const,
}

// Hook pattern (following existing hooks)
export function useCategories(options: UseCategoriesOptions = {}) {
  return useQuery({
    queryKey: queryKeys.categories.list(),
    queryFn: () => categoryAPI.getAll(),
    staleTime: CACHE_TIMES.categories.staleTime,
    // ...
  });
}
```

---

## 3. Category System Architecture

### 3.1 Database Schema Design

```
┌─────────────────┐     ┌──────────────────────┐     ┌─────────────────┐
│   categories    │     │  lecture_categories  │     │    lectures     │
├─────────────────┤     ├──────────────────────┤     ├─────────────────┤
│ id (PK)         │────<│ category_id (FK)     │>────│ id (PK)         │
│ name            │     │ lecture_id (FK)      │     │ title           │
│ slug            │     │ is_primary           │     │ description     │
│ description     │     │ created_at           │     │ speaker_id      │
│ icon_name       │     └──────────────────────┘     │ collection_id   │
│ color           │                                   │ genre (deprecated)
│ parent_id (FK)  │     ┌──────────────────────┐     │ ...             │
│ display_order   │     │ collection_categories│     └─────────────────┘
│ is_featured     │     ├──────────────────────┤
│ is_active       │────<│ category_id (FK)     │
│ lecture_count   │     │ collection_id (FK)   │
│ collection_count│     │ is_primary           │
│ created_at      │     │ created_at           │
│ updated_at      │     └──────────────────────┘
└─────────────────┘
```

### 3.2 SQL Migrations

```sql
-- V5__create_categories.sql

-- Categories table
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    icon_name VARCHAR(50) DEFAULT 'folder-outline',
    color VARCHAR(7) DEFAULT '#a855f7',
    parent_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    display_order INTEGER DEFAULT 0,
    is_featured BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    lecture_count INTEGER DEFAULT 0,
    collection_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Junction table for lecture-category (many-to-many)
CREATE TABLE lecture_categories (
    lecture_id BIGINT NOT NULL REFERENCES lectures(id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    is_primary BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (lecture_id, category_id)
);

-- Junction table for collection-category (many-to-many)
CREATE TABLE collection_categories (
    collection_id BIGINT NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    is_primary BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (collection_id, category_id)
);

-- Indexes for performance
CREATE INDEX idx_categories_parent ON categories(parent_id);
CREATE INDEX idx_categories_slug ON categories(slug);
CREATE INDEX idx_categories_featured ON categories(is_featured) WHERE is_featured = true;
CREATE INDEX idx_categories_active ON categories(is_active) WHERE is_active = true;
CREATE INDEX idx_categories_display_order ON categories(display_order);
CREATE INDEX idx_lecture_categories_category ON lecture_categories(category_id);
CREATE INDEX idx_lecture_categories_primary ON lecture_categories(is_primary) WHERE is_primary = true;
CREATE INDEX idx_collection_categories_category ON collection_categories(category_id);

-- Trigger function to update lecture_count
CREATE OR REPLACE FUNCTION update_category_lecture_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE categories SET lecture_count = lecture_count + 1 WHERE id = NEW.category_id;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE categories SET lecture_count = lecture_count - 1 WHERE id = OLD.category_id;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_lecture_categories_count
AFTER INSERT OR DELETE ON lecture_categories
FOR EACH ROW EXECUTE FUNCTION update_category_lecture_count();

-- Similar trigger for collection_count
CREATE OR REPLACE FUNCTION update_category_collection_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE categories SET collection_count = collection_count + 1 WHERE id = NEW.category_id;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE categories SET collection_count = collection_count - 1 WHERE id = OLD.category_id;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_collection_categories_count
AFTER INSERT OR DELETE ON collection_categories
FOR EACH ROW EXECUTE FUNCTION update_category_collection_count();
```

### 3.3 Initial Category Hierarchy

```sql
-- V6__seed_categories.sql

INSERT INTO categories (name, slug, description, icon_name, color, display_order, is_featured) VALUES
-- Top-level categories
('Quran & Tafsir', 'quran-tafsir', 'Quran recitation, interpretation, and tajweed', 'book-outline', '#10B981', 1, true),
('Islamic History', 'islamic-history', 'Seerah, companions, and Islamic civilization', 'library-outline', '#F59E0B', 2, true),
('Fiqh & Jurisprudence', 'fiqh', 'Islamic law, worship, and transactions', 'scale-outline', '#6366F1', 3, true),
('Spirituality & Heart', 'spirituality', 'Soul purification, character, and remembrance', 'heart-outline', '#EC4899', 4, true),
('Family & Relationships', 'family', 'Marriage, parenting, and family life', 'people-outline', '#8B5CF6', 5, true),
('Knowledge & Learning', 'knowledge', 'Seeking knowledge and Islamic sciences', 'school-outline', '#3B82F6', 6, false),
('Contemporary Issues', 'contemporary', 'Modern challenges and social issues', 'globe-outline', '#14B8A6', 7, false),
('Personal Development', 'personal-development', 'Productivity, mindset, and growth', 'trending-up-outline', '#F97316', 8, false);

-- Subcategories (example for first two)
INSERT INTO categories (name, slug, description, icon_name, color, parent_id, display_order) VALUES
('Quran Recitation', 'quran-recitation', 'Beautiful recitations and memorization', 'musical-notes-outline', '#10B981',
    (SELECT id FROM categories WHERE slug = 'quran-tafsir'), 1),
('Tafsir Studies', 'tafsir-studies', 'In-depth Quran interpretation', 'document-text-outline', '#10B981',
    (SELECT id FROM categories WHERE slug = 'quran-tafsir'), 2),
('Prophetic Biography', 'seerah', 'Life of Prophet Muhammad (PBUH)', 'person-outline', '#F59E0B',
    (SELECT id FROM categories WHERE slug = 'islamic-history'), 1),
('Companions', 'sahaba', 'Stories of the Sahaba', 'people-circle-outline', '#F59E0B',
    (SELECT id FROM categories WHERE slug = 'islamic-history'), 2),
('Marriage', 'marriage', 'Building strong Islamic marriages', 'heart-circle-outline', '#8B5CF6',
    (SELECT id FROM categories WHERE slug = 'family'), 1),
('Parenting', 'parenting', 'Raising righteous children', 'happy-outline', '#8B5CF6',
    (SELECT id FROM categories WHERE slug = 'family'), 2);
```

### 3.4 Handling Multiple Categories

**Design Principles:**

1. **Primary Category**: Each lecture/collection has ONE primary category for main navigation
2. **Secondary Categories**: Up to 3 additional categories for cross-referencing
3. **Inheritance**: Collections pass their categories to child lectures by default
4. **Override**: Individual lectures can override collection categories

---

## 4. Backend Implementation

### 4.1 Entity Classes

```java
// Category.java
package com.elmify.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_name", length = 50)
    private String iconName = "folder-outline";

    @Column(length = 7)
    private String color = "#a855f7";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private Set<Category> subcategories = new HashSet<>();

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "is_featured")
    private Boolean isFeatured = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "lecture_count")
    private Integer lectureCount = 0;

    @Column(name = "collection_count")
    private Integer collectionCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToMany(mappedBy = "categories", fetch = FetchType.LAZY)
    private Set<Lecture> lectures = new HashSet<>();

    @ManyToMany(mappedBy = "categories", fetch = FetchType.LAZY)
    private Set<Collection> collections = new HashSet<>();

    // Getters and setters...

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

```java
// LectureCategory.java - For accessing is_primary flag
package com.elmify.backend.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "lecture_categories")
public class LectureCategory {

    @EmbeddedId
    private LectureCategoryId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("lectureId")
    @JoinColumn(name = "lecture_id")
    private Lecture lecture;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("categoryId")
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and setters...
}

@Embeddable
public class LectureCategoryId implements Serializable {
    private Long lectureId;
    private Long categoryId;

    // equals, hashCode...
}
```

```java
// Update Lecture.java - Add categories relationship via junction entity
// Instead of a direct @ManyToMany, use an explicit junction entity to allow for additional metadata (e.g., is_primary).
@OneToMany(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
private Set<LectureCategory> lectureCategories = new HashSet<>();

// Access categories via lectureCategories if needed:
// lectureCategories.stream().map(LectureCategory::getCategory).collect(Collectors.toSet());
@Deprecated // Will be removed after migration to categories
@Column(name = "genre")
private String genre;
```

### 4.2 DTOs (Records)

```java
// CategoryDto.java
package com.elmify.backend.dto;

import com.elmify.backend.entity.Category;
import java.util.List;

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
    public static CategoryDto fromEntity(Category category) {
        return new CategoryDto(
            category.getId(),
            category.getName(),
            category.getSlug(),
            category.getDescription(),
            category.getIconName(),
            category.getColor(),
            category.getParent() != null ? category.getParent().getId() : null,
            category.getLectureCount(),
            category.getCollectionCount(),
            category.getIsFeatured()
        );
    }
}

// CategoryDetailDto.java - Extended with nested data
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
    List<CollectionSummaryDto> featuredCollections
) {
    public static CategoryDetailDto fromEntity(
        Category category,
        List<Category> subcategories,
        List<Collection> featuredCollections
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
            subcategories.stream().map(CategoryDto::fromEntity).toList(),
            featuredCollections.stream().map(CollectionSummaryDto::fromEntity).toList()
        );
    }
}

// CategorySummaryDto.java - For embedding in LectureDto
public record CategorySummaryDto(
    Long id,
    String name,
    String slug,
    Boolean isPrimary
) {
    public static CategorySummaryDto fromLectureCategory(LectureCategory lc) {
        return new CategorySummaryDto(
            lc.getCategory().getId(),
            lc.getCategory().getName(),
            lc.getCategory().getSlug(),
            lc.getIsPrimary()
        );
    }
}
```

```java
// Update LectureDto.java - Add categories field
public record LectureDto(
    Long id,
    String title,
    String description,
    // ... existing fields ...
    List<CategorySummaryDto> categories  // NEW FIELD
) {
    public static LectureDto fromEntity(Lecture lecture) {
        return new LectureDto(
            lecture.getId(),
            lecture.getTitle(),
            // ... existing mappings ...
            lecture.getLectureCategories().stream()
                .map(CategorySummaryDto::fromLectureCategory)
                .toList()
        );
    }
}
```

### 4.3 Repository

```java
// CategoryRepository.java
package com.elmify.backend.repository;

import com.elmify.backend.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Find all active top-level categories (no parent)
    @Query("SELECT c FROM Category c WHERE c.isActive = true AND c.parent IS NULL ORDER BY c.displayOrder")
    List<Category> findAllTopLevel();

    // Find all featured categories
    @Query("SELECT c FROM Category c WHERE c.isActive = true AND c.isFeatured = true ORDER BY c.displayOrder")
    List<Category> findAllFeatured();

    // Find by slug with subcategories eagerly loaded
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.subcategories WHERE c.slug = :slug AND c.isActive = true")
    Optional<Category> findBySlugWithSubcategories(@Param("slug") String slug);

    // Find subcategories of a parent
    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId AND c.isActive = true ORDER BY c.displayOrder")
    List<Category> findByParentId(@Param("parentId") Long parentId);

    // Check if slug exists
    boolean existsBySlug(String slug);
}

// LectureCategoryRepository.java
package com.elmify.backend.repository;

import com.elmify.backend.entity.LectureCategory;
import com.elmify.backend.entity.LectureCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface LectureCategoryRepository extends JpaRepository<LectureCategory, LectureCategoryId> {

    @Query("SELECT lc FROM LectureCategory lc WHERE lc.lecture.id = :lectureId")
    List<LectureCategory> findByLectureId(@Param("lectureId") Long lectureId);

    @Query("SELECT lc FROM LectureCategory lc WHERE lc.category.id = :categoryId")
    List<LectureCategory> findByCategoryId(@Param("categoryId") Long categoryId);
}
```

```java
// Update LectureRepository.java - Add category filtering
@Query("""
    SELECT DISTINCT l FROM Lecture l
    LEFT JOIN FETCH l.speaker
    LEFT JOIN FETCH l.collection
    JOIN l.categories c
    WHERE c.slug = :categorySlug
    ORDER BY l.playCount DESC
""")
Page<Lecture> findByCategorySlug(@Param("categorySlug") String categorySlug, Pageable pageable);

@Query("""
    SELECT DISTINCT l FROM Lecture l
    LEFT JOIN FETCH l.speaker
    LEFT JOIN FETCH l.collection
    JOIN l.categories c
    WHERE c.slug = :categorySlug AND l.speaker.isPremium = false
    ORDER BY l.playCount DESC
""")
Page<Lecture> findByCategorySlugFreeOnly(@Param("categorySlug") String categorySlug, Pageable pageable);
```

### 4.4 Service

```java
// CategoryService.java
package com.elmify.backend.service;

import com.elmify.backend.dto.CategoryDetailDto;
import com.elmify.backend.dto.CategoryDto;
import com.elmify.backend.dto.LectureDto;
import com.elmify.backend.dto.PagedResponse;
import com.elmify.backend.entity.Category;
import com.elmify.backend.entity.Lecture;
import com.elmify.backend.exception.ResourceNotFoundException;
import com.elmify.backend.repository.CategoryRepository;
import com.elmify.backend.repository.CollectionRepository;
import com.elmify.backend.repository.LectureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final LectureRepository lectureRepository;
    private final CollectionRepository collectionRepository;
    private final PremiumFilterService premiumFilterService;

    /**
     * Get all top-level categories
     */
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAllTopLevel().stream()
            .map(CategoryDto::fromEntity)
            .toList();
    }

    /**
     * Get featured categories
     */
    public List<CategoryDto> getFeaturedCategories() {
        return categoryRepository.findAllFeatured().stream()
            .map(CategoryDto::fromEntity)
            .toList();
    }

    /**
     * Get category detail by slug
     */
    public CategoryDetailDto getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlugWithSubcategories(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + slug));

        List<Category> subcategories = categoryRepository.findByParentId(category.getId());

        // Get featured collections in this category (limit 5)
        var featuredCollections = collectionRepository
            .findByCategorySlugOrderByLectureCount(slug, Pageable.ofSize(5))
            .getContent();

        return CategoryDetailDto.fromEntity(category, subcategories, featuredCollections);
    }

    /**
     * Get lectures in a category with pagination
     */
    public PagedResponse<LectureDto> getLecturesByCategory(
        String categorySlug,
        Pageable pageable,
        boolean includePremium
    ) {
        Page<Lecture> lecturePage;

        if (includePremium) {
            lecturePage = lectureRepository.findByCategorySlug(categorySlug, pageable);
        } else {
            lecturePage = lectureRepository.findByCategorySlugFreeOnly(categorySlug, pageable);
        }

        List<LectureDto> lectureDtos = lecturePage.getContent().stream()
            .map(LectureDto::fromEntity)
            .toList();

        return PagedResponse.of(lectureDtos, lecturePage);
    }

    /**
     * Get subcategories of a category
     */
    public List<CategoryDto> getSubcategories(String parentSlug) {
        Category parent = categoryRepository.findBySlugWithSubcategories(parentSlug)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + parentSlug));

        return categoryRepository.findByParentId(parent.getId()).stream()
            .map(CategoryDto::fromEntity)
            .toList();
    }
}
```

### 4.5 Controller

```java
// CategoryController.java
package com.elmify.backend.controller;

import com.elmify.backend.dto.CategoryDetailDto;
import com.elmify.backend.dto.CategoryDto;
import com.elmify.backend.dto.LectureDto;
import com.elmify.backend.dto.PagedResponse;
import com.elmify.backend.service.CategoryService;
import com.elmify.backend.service.PremiumFilterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Category browsing and filtering")
public class CategoryController {

    private final CategoryService categoryService;
    private final PremiumFilterService premiumFilterService;

    @GetMapping
    @Operation(summary = "Get all top-level categories")
    public ResponseEntity<List<CategoryDto>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured categories")
    public ResponseEntity<List<CategoryDto>> getFeaturedCategories() {
        return ResponseEntity.ok(categoryService.getFeaturedCategories());
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get category detail by slug")
    public ResponseEntity<CategoryDetailDto> getCategoryBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(categoryService.getCategoryBySlug(slug));
    }

    @GetMapping("/{slug}/lectures")
    @Operation(summary = "Get lectures in a category")
    public ResponseEntity<PagedResponse<LectureDto>> getLecturesByCategory(
        @PathVariable String slug,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        boolean includePremium = premiumFilterService.canAccessPremium();
        return ResponseEntity.ok(
            categoryService.getLecturesByCategory(slug, pageable, includePremium)
        );
    }

    @GetMapping("/{slug}/subcategories")
    @Operation(summary = "Get subcategories of a category")
    public ResponseEntity<List<CategoryDto>> getSubcategories(@PathVariable String slug) {
        return ResponseEntity.ok(categoryService.getSubcategories(slug));
    }
}
```

### 4.6 Update Existing Lecture Endpoints

```java
// LectureController.java - Add category filter parameter
@GetMapping
@Operation(summary = "Get all lectures with optional category filter")
public ResponseEntity<PagedResponse<LectureDto>> getAllLectures(
    @RequestParam(required = false) String category,  // NEW PARAMETER
    @RequestParam(required = false) String sort,
    @PageableDefault(size = 20) Pageable pageable
) {
    if (category != null) {
        boolean includePremium = premiumFilterService.canAccessPremium();
        return ResponseEntity.ok(
            categoryService.getLecturesByCategory(category, pageable, includePremium)
        );
    }
    // ... existing logic
}
```

---

## 5. Frontend Implementation

### 5.1 API Types

```typescript
// /src/api/types.ts - Add category types

export interface CategoryResponse {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  iconName: string;
  color: string;
  parentId: number | null;
  lectureCount: number;
  collectionCount: number;
  isFeatured: boolean;
}

export interface CategoryDetailResponse extends CategoryResponse {
  subcategories: CategoryResponse[];
  featuredCollections: CollectionSummaryResponse[];
}

export interface CategorySummaryResponse {
  id: number;
  name: string;
  slug: string;
  isPrimary: boolean;
}

// Update LectureResponse to include categories
export interface LectureResponse {
  // ... existing fields
  categories: CategorySummaryResponse[];
}
```

### 5.2 API Endpoints

```typescript
// /src/api/endpoints/categories.ts
import { apiClient } from '../client';
import type {
  CategoryResponse,
  CategoryDetailResponse,
  PagedResponse,
  LectureResponse
} from '../types';

export const categoryAPI = {
  /**
   * Get all top-level categories
   */
  getAll: async () => {
    return apiClient.get<CategoryResponse[]>('/categories');
  },

  /**
   * Get featured categories
   */
  getFeatured: async () => {
    return apiClient.get<CategoryResponse[]>('/categories/featured');
  },

  /**
   * Get category detail by slug
   */
  getBySlug: async (slug: string) => {
    return apiClient.get<CategoryDetailResponse>(`/categories/${slug}`);
  },

  /**
   * Get lectures in a category
   */
  getLectures: async (
    slug: string,
    params: { page?: number; size?: number; sort?: string } = {}
  ) => {
    const searchParams = new URLSearchParams();
    if (params.page) searchParams.set('page', params.page.toString());
    if (params.size) searchParams.set('size', params.size.toString());
    if (params.sort) searchParams.set('sort', params.sort);

    const query = searchParams.toString();
    return apiClient.get<PagedResponse<LectureResponse>>(
      `/categories/${slug}/lectures${query ? `?${query}` : ''}`
    );
  },

  /**
   * Get subcategories
   */
  getSubcategories: async (slug: string) => {
    return apiClient.get<CategoryResponse[]>(`/categories/${slug}/subcategories`);
  },
};
```

### 5.3 Query Keys

```typescript
// /src/queries/keys.ts - Add category keys

export const queryKeys = {
  // ... existing keys (lectures, collections, speakers, etc.)

  categories: {
    all: ['categories'] as const,
    list: () => [...queryKeys.categories.all, 'list'] as const,
    featured: () => [...queryKeys.categories.all, 'featured'] as const,
    detail: (slug: string) => [...queryKeys.categories.all, 'detail', slug] as const,
    lectures: (slug: string, params?: object) =>
      [...queryKeys.categories.all, slug, 'lectures', params] as const,
    subcategories: (slug: string) =>
      [...queryKeys.categories.all, slug, 'subcategories'] as const,
  },
};
```

### 5.4 Cache Times

```typescript
// /src/queries/client.ts - Add category cache times

export const CACHE_TIMES = {
  // ... existing cache times

  categories: {
    staleTime: 5 * 60 * 1000,    // 5 minutes - categories rarely change
    gcTime: 60 * 60 * 1000,      // 1 hour
  },
};
```

### 5.5 Query Hooks

```typescript
// /src/queries/hooks/categories.ts
import { useQuery, useInfiniteQuery } from '@tanstack/react-query';
import { categoryAPI } from '@/api/endpoints/categories';
import { queryKeys } from '@/queries/keys';
import { CACHE_TIMES } from '@/queries/client';

interface UseCategoriesOptions {
  enabled?: boolean;
}

/**
 * Get all top-level categories
 */
export function useCategories(options: UseCategoriesOptions = {}) {
  const { enabled = true } = options;

  return useQuery({
    queryKey: queryKeys.categories.list(),
    queryFn: async () => {
      const response = await categoryAPI.getAll();
      if (response.error) throw new Error(response.error);
      return response.data;
    },
    enabled,
    staleTime: CACHE_TIMES.categories.staleTime,
    gcTime: CACHE_TIMES.categories.gcTime,
  });
}

/**
 * Get featured categories
 */
export function useFeaturedCategories(options: UseCategoriesOptions = {}) {
  const { enabled = true } = options;

  return useQuery({
    queryKey: queryKeys.categories.featured(),
    queryFn: async () => {
      const response = await categoryAPI.getFeatured();
      if (response.error) throw new Error(response.error);
      return response.data;
    },
    enabled,
    staleTime: CACHE_TIMES.categories.staleTime,
    gcTime: CACHE_TIMES.categories.gcTime,
  });
}

/**
 * Get category detail by slug
 */
export function useCategoryDetail(slug: string, options: UseCategoriesOptions = {}) {
  const { enabled = true } = options;

  return useQuery({
    queryKey: queryKeys.categories.detail(slug),
    queryFn: async () => {
      const response = await categoryAPI.getBySlug(slug);
      if (response.error) throw new Error(response.error);
      return response.data;
    },
    enabled: enabled && !!slug,
    staleTime: CACHE_TIMES.categories.staleTime,
    gcTime: CACHE_TIMES.categories.gcTime,
  });
}

/**
 * Get lectures in a category with infinite scroll
 */
export function useCategoryLectures(slug: string, options: UseCategoriesOptions = {}) {
  const { enabled = true } = options;

  return useInfiniteQuery({
    queryKey: queryKeys.categories.lectures(slug),
    queryFn: async ({ pageParam = 0 }) => {
      const response = await categoryAPI.getLectures(slug, {
        page: pageParam,
        size: 20
      });
      if (response.error) throw new Error(response.error);
      return response.data;
    },
    enabled: enabled && !!slug,
    getNextPageParam: (lastPage) => {
      if (!lastPage?.pagination?.hasNext) return undefined;
      return lastPage.pagination.currentPage + 1;
    },
    initialPageParam: 0,
  });
}

/**
 * Get subcategories
 */
export function useSubcategories(slug: string, options: UseCategoriesOptions = {}) {
  const { enabled = true } = options;

  return useQuery({
    queryKey: queryKeys.categories.subcategories(slug),
    queryFn: async () => {
      const response = await categoryAPI.getSubcategories(slug);
      if (response.error) throw new Error(response.error);
      return response.data;
    },
    enabled: enabled && !!slug,
    staleTime: CACHE_TIMES.categories.staleTime,
    gcTime: CACHE_TIMES.categories.gcTime,
  });
}
```

### 5.6 UI Types

```typescript
// /src/types/ui.ts - Add UI types for categories

export interface UICategory {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  iconName: string;
  color: string;
  lectureCount: number;
  collectionCount: number;
  isFeatured: boolean;
}

export interface UICategoryDetail extends UICategory {
  subcategories: UICategory[];
  featuredCollections: UICollectionSummary[];
}
```

---

## 6. UI/UX Design

### 6.1 Screen Hierarchy

```
Browse Tab (Enhanced)
├── Search Bar
├── Featured Categories (Horizontal Scroll)
├── Speakers Section (Existing)
├── Categories Grid (New)
│   └── All top-level categories
└── Collections Section (Existing)

Category Detail Screen (New)
├── Category Header (Icon, Name, Description, Counts)
├── Subcategories (Horizontal chips)
├── Featured Collections (Horizontal scroll)
└── All Lectures (Infinite scroll list)
```

### 6.2 Screen Wireframes

#### A. Enhanced Browse Tab

```
┌─────────────────────────────────────┐
│  Browse                    [Search] │
├─────────────────────────────────────┤
│                                     │
│  Featured Categories                │
│  ┌───────┐ ┌───────┐ ┌───────┐     │
│  │ 📖    │ │ 🏛️    │ │ 💝    │ →   │
│  │Quran  │ │History│ │Family │     │
│  │  156  │ │  89   │ │  124  │     │
│  └───────┘ └───────┘ └───────┘     │
│                                     │
│  Popular Speakers              See All│
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐   │
│  │     │ │     │ │     │ │     │   │
│  └─────┘ └─────┘ └─────┘ └─────┘   │
│                                     │
│  Browse by Category                 │
│  ┌───────────┬───────────┐         │
│  │    📖     │    🏛️     │         │
│  │  Quran &  │  Islamic  │         │
│  │  Tafsir   │  History  │         │
│  │   156     │    89     │         │
│  ├───────────┼───────────┤         │
│  │    ⚖️     │    💎     │         │
│  │   Fiqh    │Spirituality│         │
│  │    67     │    93     │         │
│  └───────────┴───────────┘         │
│                                     │
│  Recent Collections            See All│
│  ┌─────────────────────────────┐   │
│  │ [IMG] Marriage Series       │   │
│  └─────────────────────────────┘   │
│                                     │
└─────────────────────────────────────┘
```

#### B. Category Detail Screen

```
┌─────────────────────────────────────┐
│  ←  Family & Relationships          │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐   │
│  │         👨‍👩‍👧‍👦                    │   │
│  │  Family & Relationships      │   │
│  │  124 lectures • 8 series     │   │
│  │                              │   │
│  │ Building strong families and │   │
│  │ nurturing relationships      │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────┐ ┌─────────┐ ┌────────┐│
│  │Marriage │ │Parenting│ │ Youth  ││
│  └─────────┘ └─────────┘ └────────┘│
│                                     │
│  Popular Series                     │
│  ┌─────────────────────────────┐   │
│  │ [IMG] Marriage Masterclass  │   │
│  │       Sheikh Ahmad • 24 eps │   │
│  ├─────────────────────────────┤   │
│  │ [IMG] Parenting in Islam    │   │
│  │       Dr. Yasmin • 12 eps   │   │
│  └─────────────────────────────┘   │
│                                     │
│  All Lectures            [Sort ▼]   │
│  ┌─────────────────────────────┐   │
│  │ Communication in Marriage   │   │
│  │ Marriage • 45 min       [▶] │   │
│  ├─────────────────────────────┤   │
│  │ Raising Confident Children  │   │
│  │ Parenting • 38 min      [▶] │   │
│  └─────────────────────────────┘   │
│              ↓ Load More            │
└─────────────────────────────────────┘
```

#### C. Category Chips on Lecture Cards

```
┌─────────────────────────────────────┐
│ [Thumbnail]                         │
│                                     │
│ Building a Successful Marriage      │
│ Sheikh Ahmad Al-Mahmoud             │
│                                     │
│ ┌──────────┐ ┌────────────┐        │
│ │ Marriage │ │Spirituality│        │
│ └──────────┘ └────────────┘        │
│                                     │
│ 45 min • 12K plays              [▶] │
└─────────────────────────────────────┘
```

### 6.3 Component Structure

```
/src/components/categories/
├── CategoryCard.tsx          // Grid item for category
├── CategoryChip.tsx          // Small pill for lecture cards
├── CategoryHeader.tsx        // Hero header for detail screen
├── CategoryGrid.tsx          // 2-column grid of categories
├── FeaturedCategories.tsx    // Horizontal scroll of featured
└── SubcategoryList.tsx       // Horizontal chips for subcategories
```

### 6.4 Navigation

```typescript
// App router structure
/browse                          // Enhanced with categories
/category/[slug]                 // Category detail
/category/[slug]/lectures        // Optional: dedicated lecture list
```

---

## 7. Implementation Plan

### 7.1 Phase 1: Backend Foundation

#### Database & Migrations
- [ ] Create `V5__create_categories.sql` migration
- [ ] Create `V6__seed_categories.sql` with initial data
- [ ] Run migrations on dev environment
- [ ] Verify triggers work correctly

#### Entities
- [ ] Create `Category.java` entity
- [ ] Create `LectureCategory.java` entity (for is_primary access)
- [ ] Create `CollectionCategory.java` entity
- [ ] Update `Lecture.java` with categories relationship
- [ ] Update `Collection.java` with categories relationship
- [ ] Mark `genre` field as `@Deprecated`

#### DTOs
- [ ] Create `CategoryDto.java` record
- [ ] Create `CategoryDetailDto.java` record
- [ ] Create `CategorySummaryDto.java` record
- [ ] Update `LectureDto.java` to include categories

#### Repository
- [ ] Create `CategoryRepository.java`
- [ ] Create `LectureCategoryRepository.java`
- [ ] Add `findByCategorySlug` to `LectureRepository`
- [ ] Add `findByCategorySlug` to `CollectionRepository`

#### Service
- [ ] Create `CategoryService.java`
- [ ] Implement `getAllCategories()`
- [ ] Implement `getFeaturedCategories()`
- [ ] Implement `getCategoryBySlug()`
- [ ] Implement `getLecturesByCategory()`
- [ ] Implement `getSubcategories()`

#### Controller
- [ ] Create `CategoryController.java`
- [ ] Implement `GET /categories`
- [ ] Implement `GET /categories/featured`
- [ ] Implement `GET /categories/{slug}`
- [ ] Implement `GET /categories/{slug}/lectures`
- [ ] Implement `GET /categories/{slug}/subcategories`
- [ ] Add OpenAPI documentation

#### Integration
- [ ] Update `LectureController` with category filter
- [ ] Integrate with `PremiumFilterService`
- [ ] Add to Swagger API docs

### 7.2 Phase 2: Frontend Foundation

#### API Layer
- [ ] Add category types to `/api/types.ts`
- [ ] Create `/api/endpoints/categories.ts`
- [ ] Update `LectureResponse` type with categories

#### Query Layer
- [ ] Add category keys to `/queries/keys.ts`
- [ ] Add category cache times to `/queries/client.ts`
- [ ] Create `/queries/hooks/categories.ts`
- [ ] Implement `useCategories()`
- [ ] Implement `useFeaturedCategories()`
- [ ] Implement `useCategoryDetail()`
- [ ] Implement `useCategoryLectures()`

#### UI Types
- [ ] Add `UICategory` to `/types/ui.ts`
- [ ] Add `UICategoryDetail` to `/types/ui.ts`

### 7.3 Phase 3: Frontend Components

#### Core Components
- [ ] Create `CategoryCard.tsx`
- [ ] Create `CategoryChip.tsx`
- [ ] Create `CategoryHeader.tsx`
- [ ] Create `CategoryGrid.tsx`
- [ ] Create `FeaturedCategories.tsx`
- [ ] Create `SubcategoryList.tsx`

#### Screens
- [ ] Create `/category/[slug].tsx` screen
- [ ] Add loading skeleton for category screen
- [ ] Add empty state for no lectures
- [ ] Implement infinite scroll for lectures

### 7.4 Phase 4: Integration

#### Browse Tab Enhancement
- [ ] Add `FeaturedCategories` section to browse.tsx
- [ ] Add `CategoryGrid` section to browse.tsx
- [ ] Prefetch categories on app launch

#### Lecture Cards
- [ ] Add `CategoryChip` to lecture cards
- [ ] Update `LectureListItem` component
- [ ] Update `LectureCard` component

#### Navigation
- [ ] Add category routes to app router
- [ ] Implement deep linking for categories
- [ ] Add back navigation from category detail

### 7.5 Phase 5: Data Migration

#### Content Categorization
- [ ] Create script to map existing `genre` values to categories
- [ ] Manually categorize remaining lectures
- [ ] Assign primary categories to all lectures
- [ ] Assign categories to collections
- [ ] Verify lecture counts are correct

### 7.6 Phase 6: Testing

#### Backend Tests
- [ ] Unit tests for `CategoryService`
- [ ] Unit tests for category DTOs
- [ ] Integration tests for `CategoryController`
- [ ] Test premium filtering with categories
- [ ] Test pagination for category lectures
- [ ] Load test category endpoints

#### Frontend Tests
- [ ] Component tests for `CategoryCard`
- [ ] Component tests for `CategoryChip`
- [ ] Hook tests for `useCategories`
- [ ] Hook tests for `useCategoryLectures`
- [ ] Navigation tests

#### E2E Tests
- [ ] Test category browsing flow
- [ ] Test category → lecture navigation
- [ ] Test infinite scroll loading
- [ ] Test on slow network

### 7.7 Phase 7: Production Deployment

#### Pre-deployment
- [ ] Run migrations on staging
- [ ] Categorize all production lectures
- [ ] Performance test with production data
- [ ] Update API documentation

#### Deployment
- [ ] Deploy backend changes
- [ ] Run migrations on production
- [ ] Deploy frontend changes
- [ ] Monitor for errors

#### Post-deployment
- [ ] Verify category counts
- [ ] Test all category screens
- [ ] Monitor API performance
- [ ] Set up analytics events

---

## 8. Additional Improvements

### 8.1 Future: Server-Side Caching with Redis

```java
// When Redis is added to the backend
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Cacheable(value = "categories", key = "'all'")
    public List<CategoryDto> getAllCategories() {
        // ...
    }

    @CacheEvict(value = "categories", allEntries = true)
    public void invalidateCategoryCache() {
        // Called when categories are updated
    }
}
```

### 8.2 Search Integration

```typescript
// Enhanced search with category faceting
interface SearchResponse {
  results: LectureResponse[];
  facets: {
    categories: {
      slug: string;
      name: string;
      count: number;
    }[];
  };
}

// UI: Show category filters in search results
```

### 8.3 Recommendations Engine

```typescript
// Based on listening history
interface RecommendationRequest {
  userId: string;
  categoryWeights: Map<string, number>; // category -> listening time
}

// Endpoints
GET /api/v1/recommendations/categories      // Suggested categories
GET /api/v1/recommendations/in-category/:slug // Similar in category
```

### 8.4 Analytics Events

```typescript
// Track category engagement
analytics.track('category_viewed', {
  categorySlug: 'marriage',
  categoryName: 'Marriage',
  source: 'browse_grid' | 'featured' | 'search' | 'deep_link',
});

analytics.track('category_lecture_played', {
  categorySlug: 'marriage',
  lectureId: '123',
});
```

### 8.5 Admin Panel (Future)

```
POST   /api/v1/admin/categories           // Create
PUT    /api/v1/admin/categories/:id       // Update
DELETE /api/v1/admin/categories/:id       // Soft delete
POST   /api/v1/admin/categories/reorder   // Change display order
POST   /api/v1/admin/lectures/:id/assign-categories
```

---

## Appendix

### A. Category Icons (Ionicons)

| Category | Icon Name |
|----------|-----------|
| Quran & Tafsir | `book-outline` |
| Islamic History | `library-outline` |
| Fiqh | `scale-outline` |
| Spirituality | `heart-outline` |
| Family | `people-outline` |
| Knowledge | `school-outline` |
| Contemporary | `globe-outline` |
| Personal Development | `trending-up-outline` |

### B. Category Colors

| Category | Hex | Tailwind |
|----------|-----|----------|
| Quran & Tafsir | `#10B981` | `emerald-500` |
| Islamic History | `#F59E0B` | `amber-500` |
| Fiqh | `#6366F1` | `indigo-500` |
| Spirituality | `#EC4899` | `pink-500` |
| Family | `#8B5CF6` | `violet-500` |
| Knowledge | `#3B82F6` | `blue-500` |
| Contemporary | `#14B8A6` | `teal-500` |
| Personal Development | `#F97316` | `orange-500` |

### C. Genre Migration Mapping

| Existing Genre | Target Category |
|----------------|-----------------|
| "Quran" | quran-tafsir |
| "Seerah" | seerah |
| "Fiqh" | fiqh |
| "Aqeedah" | spirituality |
| "Family" | family |
| "Marriage" | marriage |
| (unmapped) | Requires manual categorization |

---

## Summary

This category system provides:

1. **Clean Architecture**: Follows existing codebase patterns (DTOs as records, `@Transactional`, `PagedResponse`)
2. **No Redundancy**: Reuses existing services (`PremiumFilterService`), patterns (query keys factory), and components
3. **Scalable Design**: Normalized database, hierarchical categories, denormalized counts
4. **Professional UX**: Inspired by Spotify/Apple Podcasts genre browsing
5. **Future-Ready**: Prepared for Redis caching, search faceting, recommendations

The implementation is broken into 7 phases allowing incremental delivery while maintaining code quality and existing patterns.
