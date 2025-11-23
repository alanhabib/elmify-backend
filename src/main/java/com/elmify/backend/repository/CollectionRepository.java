package com.elmify.backend.repository;

import com.elmify.backend.entity.Collection;
import com.elmify.backend.entity.Speaker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, Long> {
    
    List<Collection> findBySpeaker(Speaker speaker);
    
    List<Collection> findBySpeakerId(Long speakerId);
    
    Optional<Collection> findBySpeakerAndTitle(Speaker speaker, String title);
    
    Optional<Collection> findBySpeakerIdAndTitle(Long speakerId, String title);
    
    List<Collection> findByTitleContainingIgnoreCase(String title);
    
    @Query("SELECT c FROM Collection c WHERE c.speaker.name ILIKE %:speakerName%")
    List<Collection> findBySpeakerNameContaining(@Param("speakerName") String speakerName);
    
    @Query("SELECT c FROM Collection c WHERE c.title ILIKE %:title% OR c.speaker.name ILIKE %:title%")
    List<Collection> searchByTitleOrSpeakerName(@Param("title") String title);
    
    @Query("SELECT c FROM Collection c ORDER BY c.speaker.name ASC, c.title ASC")
    List<Collection> findAllOrderBySpeakerAndTitle();
    
    @Query("SELECT c FROM Collection c JOIN FETCH c.speaker ORDER BY c.speaker.name ASC, c.title ASC")
    List<Collection> findAllWithSpeakerOrderBySpeakerAndTitle();
    
    @Query("SELECT c, COUNT(l) FROM Collection c LEFT JOIN c.lectures l GROUP BY c ORDER BY c.speaker.name ASC, c.title ASC")
    List<Object[]> findAllWithSpeakerAndLectureCountOrderBySpeakerAndTitle();
    
    @Query("SELECT c FROM Collection c WHERE c.speaker.id = :speakerId ORDER BY c.title ASC")
    List<Collection> findBySpeakerIdOrderByTitle(@Param("speakerId") Long speakerId);
    
    @Query("SELECT c FROM Collection c JOIN FETCH c.speaker WHERE c.speaker.id = :speakerId ORDER BY c.title ASC")
    List<Collection> findBySpeakerIdWithSpeakerOrderByTitle(@Param("speakerId") Long speakerId);
    
    List<Collection> findByYear(Integer year);
    
    @Query("SELECT c FROM Collection c WHERE c.year BETWEEN :startYear AND :endYear")
    List<Collection> findByYearBetween(@Param("startYear") Integer startYear, @Param("endYear") Integer endYear);
    
    boolean existsBySpeakerAndTitle(Speaker speaker, String title);
    
    @Query("SELECT c FROM Collection c JOIN FETCH c.speaker WHERE c.id = :id")
    Optional<Collection> findByIdWithSpeaker(@Param("id") Long id);

    /**
     * Find collections by category slug with speaker eagerly loaded.
     * Used for featured collections in category detail view.
     *
     * @param categorySlug The category slug.
     * @param pageable Pagination information.
     * @return A Page of Collection entities in the category.
     */
    @Query(value = "SELECT DISTINCT c FROM Collection c " +
            "LEFT JOIN FETCH c.speaker " +
            "JOIN c.collectionCategories cc " +
            "WHERE cc.category.slug = :categorySlug " +
            "ORDER BY c.lectureCount DESC",
            countQuery = "SELECT COUNT(DISTINCT c) FROM Collection c " +
                    "JOIN c.collectionCategories cc " +
                    "WHERE cc.category.slug = :categorySlug")
    Page<Collection> findByCategorySlug(@Param("categorySlug") String categorySlug, Pageable pageable);
}