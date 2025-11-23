package com.elmify.backend.repository;

import com.elmify.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Find all active top-level categories (no parent).
     */
    @Query("SELECT c FROM Category c WHERE c.isActive = true AND c.parent IS NULL ORDER BY c.displayOrder")
    List<Category> findAllTopLevel();

    /**
     * Find all featured categories with parent eagerly loaded.
     */
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.parent WHERE c.isActive = true AND c.isFeatured = true ORDER BY c.displayOrder")
    List<Category> findAllFeatured();

    /**
     * Find by slug with parent eagerly loaded.
     */
    Optional<Category> findBySlugAndIsActiveTrue(String slug);

    /**
     * Find by slug with subcategories and parent eagerly loaded.
     */
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.subcategories LEFT JOIN FETCH c.parent WHERE c.slug = :slug AND c.isActive = true")
    Optional<Category> findBySlugWithSubcategories(@Param("slug") String slug);

    /**
     * Find subcategories of a parent.
     */
    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId AND c.isActive = true ORDER BY c.displayOrder")
    List<Category> findByParentId(@Param("parentId") Long parentId);

    /**
     * Check if slug exists.
     */
    boolean existsBySlug(String slug);
}
