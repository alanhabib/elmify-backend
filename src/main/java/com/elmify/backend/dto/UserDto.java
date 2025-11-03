package com.elmify.backend.dto;

import com.elmify.backend.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;

/**
 * A Data Transfer Object representing a user's public profile.
 * This is the standard representation of a user sent from the API to the client.
 *
 * @param id The internal database ID of the user.
 * @param clerkId The user's unique Clerk ID.
 * @param email The user's email address.
 * @param displayName The user's display name.
 * @param profileImageUrl The URL for the user's profile image.
 * @param isPremium The user's premium status.
 * @param preferences The user's preferences (theme, goals, notifications, etc.)
 * @param createdAt The timestamp when the user was created.
 */
public record UserDto(
        Long id,
        String clerkId,
        String email,
        String displayName,
        String profileImageUrl,
        boolean isPremium,
        UserPreferencesDto preferences,
        OffsetDateTime createdAt
) {
    /**
     * A static factory method to create a UserDto from a User entity.
     * This safely converts the database object into a client-facing DTO.
     *
     * @param user The User entity from the database.
     * @return A new UserDto instance.
     */
    public static UserDto fromEntity(User user) {
        UserPreferencesDto prefs = null;
        if (user.getPreferences() != null && !user.getPreferences().isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                prefs = mapper.readValue(user.getPreferences(), UserPreferencesDto.class);
            } catch (Exception e) {
                // Return null if parsing fails, client should use defaults
                prefs = null;
            }
        }

        return new UserDto(
                user.getId(),
                user.getClerkId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getProfileImageUrl(),
                user.isPremium(),
                prefs,
                user.getCreatedAt()
        );
    }
}