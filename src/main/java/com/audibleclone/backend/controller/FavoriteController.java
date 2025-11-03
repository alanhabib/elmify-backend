package com.audibleclone.backend.controller;

import com.audibleclone.backend.dto.FavoriteDto;
import com.audibleclone.backend.dto.PagedResponse;
import com.audibleclone.backend.entity.Favorite;
import com.audibleclone.backend.service.FavoriteService;
import com.audibleclone.backend.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for managing user favorites
 * All endpoints require authentication
 */
@RestController
@RequestMapping("/api/v1/favorites")
@Tag(name = "Favorites", description = "Endpoints for managing user favorites (Authentication Required)")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final StorageService storageService;

    @GetMapping
    @Operation(summary = "Get User Favorites",
            description = "Retrieves all favorites for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved favorites")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @Transactional(readOnly = true)
    public ResponseEntity<PagedResponse<FavoriteDto>> getUserFavorites(
            @AuthenticationPrincipal Jwt jwt,
            Pageable pageable) {
        String userId = jwt.getSubject();

        Page<Favorite> favorites = favoriteService.getUserFavorites(userId, pageable);
        Page<FavoriteDto> favoriteDtos = favorites.map(fav -> FavoriteDto.fromEntity(fav, storageService));
        PagedResponse<FavoriteDto> response = PagedResponse.from(favoriteDtos);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/check/{lectureId}")
    @Operation(summary = "Check if Lecture is Favorited",
            description = "Check if a specific lecture is in user's favorites")
    @ApiResponse(responseCode = "200", description = "Successfully checked favorite status")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    public ResponseEntity<Map<String, Boolean>> checkFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Lecture ID") @PathVariable Long lectureId) {
        String userId = jwt.getSubject();
        boolean isFavorited = favoriteService.isFavorited(userId, lectureId);

        return ResponseEntity.ok(Map.of("isFavorited", isFavorited));
    }

    @PostMapping("/{lectureId}")
    @Operation(summary = "Add to Favorites",
            description = "Add a lecture to user's favorites")
    @ApiResponse(responseCode = "201", description = "Successfully added to favorites")
    @ApiResponse(responseCode = "200", description = "Already in favorites")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Lecture not found")
    @Transactional
    public ResponseEntity<FavoriteDto> addFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Lecture ID") @PathVariable Long lectureId) {
        String userId = jwt.getSubject();

        try {
            Favorite favorite = favoriteService.addFavorite(userId, lectureId);
            FavoriteDto dto = FavoriteDto.fromEntity(favorite, storageService);

            // Return 201 if newly created, 200 if already existed
            HttpStatus status = favorite.getCreatedAt() != null ? HttpStatus.CREATED : HttpStatus.OK;
            return ResponseEntity.status(status).body(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/{lectureId}")
    @Operation(summary = "Remove from Favorites",
            description = "Remove a lecture from user's favorites")
    @ApiResponse(responseCode = "204", description = "Successfully removed from favorites")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @Transactional
    public ResponseEntity<Void> removeFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Lecture ID") @PathVariable Long lectureId) {
        String userId = jwt.getSubject();
        favoriteService.removeFavorite(userId, lectureId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    @Operation(summary = "Get Favorites Count",
            description = "Get total number of favorites for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved count")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    public ResponseEntity<Map<String, Long>> getFavoriteCount(
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        long count = favoriteService.getFavoriteCount(userId);

        return ResponseEntity.ok(Map.of("count", count));
    }
}
