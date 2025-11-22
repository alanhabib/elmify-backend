package com.elmify.backend.service;

import com.elmify.backend.entity.Category;
import com.elmify.backend.entity.Collection;
import com.elmify.backend.entity.Lecture;
import com.elmify.backend.repository.CategoryRepository;
import com.elmify.backend.repository.CollectionRepository;
import com.elmify.backend.repository.LectureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for retrieving Category entities and related content.
 * This service is read-only and respects premium filtering for content queries.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final LectureRepository lectureRepository;
    private final CollectionRepository collectionRepository;
    private final PremiumFilterService premiumFilterService;

    /**
     * Retrieves all active top-level categories.
     *
     * @return List of top-level Category entities.
     */
    public List<Category> getAllTopLevelCategories() {
        return categoryRepository.findAllTopLevel();
    }

    /**
     * Retrieves all featured categories.
     *
     * @return List of featured Category entities.
     */
    public List<Category> getFeaturedCategories() {
        return categoryRepository.findAllFeatured();
    }

    /**
     * Retrieves a category by its slug with subcategories loaded.
     *
     * @param slug The category slug.
     * @return Optional containing the Category if found.
     */
    public Optional<Category> getCategoryBySlug(String slug) {
        return categoryRepository.findBySlugWithSubcategories(slug);
    }

    /**
     * Retrieves subcategories of a parent category.
     *
     * @param parentId The parent category ID.
     * @return List of subcategory entities.
     */
    public List<Category> getSubcategories(Long parentId) {
        return categoryRepository.findByParentId(parentId);
    }

    /**
     * Retrieves paginated lectures for a category.
     * Filters out premium lectures for non-premium users.
     *
     * @param categorySlug The category slug.
     * @param pageable Pagination information.
     * @return A Page of Lecture entities.
     */
    public Page<Lecture> getLecturesByCategory(String categorySlug, Pageable pageable) {
        boolean isPremiumUser = premiumFilterService.isCurrentUserPremium();

        Page<Lecture> lectures;
        if (isPremiumUser) {
            lectures = lectureRepository.findByCategorySlug(categorySlug, pageable);
        } else {
            lectures = lectureRepository.findByCategorySlugFreeOnly(categorySlug, pageable);
        }

        return lectures;
    }

    /**
     * Retrieves paginated collections for a category.
     * Used for featured collections in category detail view.
     *
     * @param categorySlug The category slug.
     * @param pageable Pagination information.
     * @return A Page of Collection entities.
     */
    public Page<Collection> getCollectionsByCategory(String categorySlug, Pageable pageable) {
        return collectionRepository.findByCategorySlug(categorySlug, pageable);
    }

    /**
     * Retrieves featured collections for a category (limited to top 5).
     *
     * @param categorySlug The category slug.
     * @return List of top Collection entities in the category.
     */
    public List<Collection> getFeaturedCollectionsForCategory(String categorySlug) {
        Page<Collection> collections = collectionRepository.findByCategorySlug(
                categorySlug,
                PageRequest.of(0, 5)
        );
        return collections.getContent();
    }
}
