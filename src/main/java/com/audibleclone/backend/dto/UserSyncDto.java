package com.audibleclone.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * A Data Transfer Object used to sync user information from Clerk.
 * This is the expected request body for the user synchronization endpoint.
 *
 * @param clerkId The unique identifier for the user from Clerk.
 * @param email The user's email address.
 * @param displayName The user's display name or full name.
 * @param profileImageUrl A URL to the user's profile image.
 */
public record UserSyncDto(
        @NotBlank String clerkId,
        @Email String email,
        String displayName,
        String profileImageUrl
) {}