package com.audibleclone.backend.repository;

import com.audibleclone.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the User entity.
 * This interface provides CRUD operations and custom queries for the 'users' table.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // --- Core User Lookup Methods ---

    /**
     * Finds a user by their unique Clerk ID. This is the primary method for retrieving a user
     * after they have been authenticated by Clerk.
     *
     * @param clerkId The unique ID provided by Clerk.com.
     * @return An Optional containing the User if found.
     */
    Optional<User> findByClerkId(String clerkId);

    /**
     * Finds a user by their email address.
     *
     * @param email The user's email.
     * @return An Optional containing the User if found.
     */
    Optional<User> findByEmail(String email);


    // --- Existence Checks ---

    /**
     * Checks if a user exists with the given Clerk ID.
     *
     * @param clerkId The unique ID from Clerk.com.
     * @return true if a user exists, false otherwise.
     */
    boolean existsByClerkId(String clerkId);

    /**
     * Checks if a user exists with the given email address.
     *
     * @param email The user's email.
     * @return true if a user exists, false otherwise.
     */
    boolean existsByEmail(String email);


    // --- Queries Based on User Properties ---

    /**
     * Finds all users based on their premium status.
     *
     * @param isPremium The premium status to search for.
     * @return A List of users matching the premium status.
     */
    List<User> findByIsPremium(boolean isPremium);

    /**
     * Finds a paginated list of users based on their premium status.
     *
     * @param isPremium The premium status to search for.
     * @param pageable  Pagination information.
     * @return A Page of users matching the premium status.
     */
    Page<User> findByIsPremium(boolean isPremium, Pageable pageable);


    // --- Date-Based Queries ---

    /**
     * Finds all users created within a specific date range.
     * Note: Uses OffsetDateTime to match the 'timestamp with time zone' type in the entity.
     *
     * @param startDate The start of the date range (inclusive).
     * @param endDate   The end of the date range (inclusive).
     * @return A List of users created within the specified range.
     */
    List<User> findByCreatedAtBetween(OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * Finds users who have joined since a certain date, ordered by most recent first.
     *
     * @param since The cutoff date and time.
     * @return A List of recently joined users.
     */
    @Query("SELECT u FROM User u WHERE u.createdAt >= :since ORDER BY u.createdAt DESC")
    List<User> findRecentlyJoinedUsers(@Param("since") OffsetDateTime since);

}