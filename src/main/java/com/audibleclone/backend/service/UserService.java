package com.audibleclone.backend.service;

import com.audibleclone.backend.dto.UserDto;
import com.audibleclone.backend.dto.UserPreferencesDto;
import com.audibleclone.backend.dto.UserSyncDto;
import com.audibleclone.backend.entity.User;
import com.audibleclone.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service layer for managing User entities.
 *
 * Responsibilities:
 * - Syncing user data from Clerk (creating and updating local user profiles).
 * - Retrieving application-specific user data.
 * - Updating user preferences and premium status.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Synchronizes user data from Clerk into the local database.
     * If the user exists, their profile is updated. If not, a new user is created.
     *
     * @param userSyncDto DTO containing the latest user info from Clerk (INPUT).
     * @return The created or updated User entity, converted to a DTO (OUTPUT).
     */
    public UserDto syncUser(UserSyncDto userSyncDto) {
        User user = userRepository.findByClerkId(userSyncDto.clerkId())
                .map(existingUser -> {
                    // User exists, update their mutable information
                    log.debug("User found with Clerk ID [{}]. Syncing profile.", userSyncDto.clerkId());
                    existingUser.setEmail(userSyncDto.email());
                    existingUser.setDisplayName(userSyncDto.displayName());
                    existingUser.setProfileImageUrl(userSyncDto.profileImageUrl());
                    return existingUser;
                })
                .orElseGet(() -> {
                    // User does not exist, create a new record
                    log.info("Creating new user with Clerk ID [{}].", userSyncDto.clerkId());
                    User newUser = new User();
                    newUser.setClerkId(userSyncDto.clerkId());
                    newUser.setEmail(userSyncDto.email());
                    newUser.setDisplayName(userSyncDto.displayName());
                    newUser.setProfileImageUrl(userSyncDto.profileImageUrl());
                    return newUser;
                });

        User savedUser = userRepository.save(user);
        return UserDto.fromEntity(savedUser);
    }

    /**
     * Finds a user by their unique Clerk ID.
     *
     * @param clerkId The Clerk user ID.
     * @return An Optional containing the UserDto if found.
     */
    @Transactional(readOnly = true)
    public Optional<UserDto> findByClerkId(String clerkId) {
        return userRepository.findByClerkId(clerkId).map(user -> UserDto.fromEntity(user));
    }

    /**
     * Finds a user by their internal database ID.
     *
     * @param id The primary key of the user.
     * @return An Optional containing the UserDto if found.
     */
    @Transactional(readOnly = true)
    public Optional<UserDto> findById(Long id) {
        return userRepository.findById(id).map(user -> UserDto.fromEntity(user));
    }

    /**
     * Retrieves a paginated list of all users.
     * @param pageable Pagination information.
     * @return A Page of UserDto objects.
     */
    @Transactional(readOnly = true)
    public Page<UserDto> findAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return users.map(user -> UserDto.fromEntity(user));
    }

    /**
     * Updates the premium status for a given user.
     *
     * @param clerkId The Clerk ID of the user to update.
     * @param isPremium The new premium status.
     */
    public void updateUserPremiumStatus(String clerkId, boolean isPremium) {
        User user = userRepository.findByClerkId(clerkId)
                .orElseThrow(() -> new RuntimeException("User not found with Clerk ID: " + clerkId)); // Replace with custom exception

        if (user.isPremium() != isPremium) {
            user.setPremium(isPremium);
            userRepository.save(user);
            log.info("Updated premium status for user [{}] to [{}].", clerkId, isPremium);
        }
    }

    /**
     * Update user preferences.
     * Preferences include theme, daily goals, notifications, etc.
     *
     * @param clerkId The Clerk ID of the user
     * @param preferencesDto The preferences to update
     * @return Updated UserDto
     */
    public UserDto updatePreferences(String clerkId, UserPreferencesDto preferencesDto) {
        User user = userRepository.findByClerkId(clerkId)
            .orElseThrow(() -> new RuntimeException("User not found with clerkId: " + clerkId));

        try {
            // Convert preferences DTO to JSON string
            String preferencesJson = objectMapper.writeValueAsString(preferencesDto);
            user.setPreferences(preferencesJson);

            User savedUser = userRepository.save(user);
            log.info("Updated preferences for user [{}]", clerkId);

            return UserDto.fromEntity(savedUser);
        } catch (Exception e) {
            log.error("Failed to update preferences for user {}", clerkId, e);
            throw new RuntimeException("Failed to update preferences: " + e.getMessage(), e);
        }
    }
}