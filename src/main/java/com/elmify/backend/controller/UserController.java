package com.elmify.backend.controller;

import com.elmify.backend.dto.UserDto;
import com.elmify.backend.dto.UserPreferencesDto;
import com.elmify.backend.dto.UserSyncDto;
import com.elmify.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling user-related operations, primarily for syncing user data
 * from an external authentication provider like Clerk.
 */
@RestController
@RequestMapping("/api/v1/users") // Standard API versioning in the path
@Tag(name = "User Management", description = "Endpoints for syncing and retrieving user information")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * This is the core endpoint for integrating with Clerk.
     * The frontend should call this endpoint after a user successfully signs up or logs in.
     * The service layer handles both creation and updates idempotently.
     */
    @PostMapping("/sync")
    @Operation(summary = "Synchronize User Data",
            description = "Creates a new user or updates an existing one based on the provided Clerk ID. This is the primary endpoint for user integration.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "User synchronized successfully")
    @ApiResponse(responseCode = "400", description = "Invalid user data provided")
    @ApiResponse(responseCode = "403", description = "ClerkId mismatch - cannot sync other users")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> syncUser(
            Authentication authentication,
            @Valid @RequestBody UserSyncDto userSyncDto) {
        String tokenClerkId = authentication.getName();

        // Security check: users can only sync their own data
        if (!tokenClerkId.equals(userSyncDto.clerkId())) {
            log.warn("ClerkId mismatch - token: {}, body: {}", tokenClerkId, userSyncDto.clerkId());
            throw new SecurityException("Cannot sync user: clerkId mismatch");
        }

        log.info("Received sync request for Clerk ID: {}", userSyncDto.clerkId());
        UserDto userDto = userService.syncUser(userSyncDto);
        return ResponseEntity.ok(userDto);
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     * The user's identity is determined from the JWT token provided in the request.
     */
    @GetMapping("/me")
    @Operation(summary = "Get Current User Profile",
            description = "Retrieves the profile of the currently authenticated user.")
    @SecurityRequirement(name = "bearerAuth") // Indicates this endpoint requires authentication
    @ApiResponse(responseCode = "200", description = "Profile retrieved successfully")
    @ApiResponse(responseCode = "404", description = "User profile not found in the database")
    @PreAuthorize("isAuthenticated()") // Ensures a user is logged in
    public ResponseEntity<UserDto> getCurrentUserProfile(Authentication authentication) {
        String clerkId = authentication.getName(); // Spring Security puts the token's subject (clerkId) here
        log.debug("Fetching profile for user: {}", clerkId);

        return userService.findByClerkId(clerkId)
                .map(ResponseEntity::ok) // If user is found, wrap in ResponseEntity.ok()
                .orElseGet(() -> ResponseEntity.notFound().build()); // If not found, return 404
    }

    /**
     * Retrieves a paginated list of all users.
     * This is an administrative endpoint and should be protected accordingly.
     */
    /**
     * Update user preferences for the authenticated user.
     * Preferences include theme, daily goals, notifications, etc.
     */
    @PutMapping("/me/preferences")
    @Operation(summary = "Update User Preferences",
            description = "Update preferences for the authenticated user")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Preferences updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid preferences data")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> updatePreferences(
            Authentication authentication,
            @Valid @RequestBody UserPreferencesDto preferencesDto) {
        String clerkId = authentication.getName();
        log.debug("Updating preferences for user: {}", clerkId);

        UserDto updatedUser = userService.updatePreferences(clerkId, preferencesDto);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping
    @Operation(summary = "Get All Users (Admin)",
            description = "Retrieves a paginated list of all users. Requires ADMIN authority.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')") // Requires the JWT to have an admin role/authority
    public ResponseEntity<Page<UserDto>> getAllUsers(Pageable pageable) {
        log.info("Admin request to get all users");
        Page<UserDto> users = userService.findAllUsers(pageable);
        return ResponseEntity.ok(users);
    }
}