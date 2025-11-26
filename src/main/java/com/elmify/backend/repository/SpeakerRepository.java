package com.elmify.backend.repository;

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
public interface SpeakerRepository extends JpaRepository<Speaker, Long> {

    Optional<Speaker> findByName(String name);

    List<Speaker> findByNameContainingIgnoreCase(String name);

    @Query("SELECT s FROM Speaker s WHERE s.name ILIKE %:searchTerm%")
    List<Speaker> searchByName(@Param("searchTerm") String searchTerm);

    @Query("SELECT s FROM Speaker s ORDER BY s.name ASC")
    List<Speaker> findAllOrderByName();

    boolean existsByName(String name);

    /**
     * Find all speakers that are not premium (for non-premium users).
     * Includes speakers where isPremium is false or null.
     */
    @Query("SELECT s FROM Speaker s WHERE s.isPremium = false OR s.isPremium IS NULL")
    Page<Speaker> findByIsPremiumFalseOrIsPremiumIsNull(Pageable pageable);
}