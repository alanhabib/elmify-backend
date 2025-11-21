package com.elmify.backend.controller;

import com.elmify.backend.dto.CollectionDto;
import com.elmify.backend.dto.PagedResponse;
import com.elmify.backend.service.CollectionService;
import com.elmify.backend.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Public, read-only REST controller for browsing audio collections.
 */
@RestController
@RequestMapping("/api/v1/collections")
@Tag(name = "Collections", description = "Public endpoints for browsing collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;
    private final StorageService storageService;

    @GetMapping
    @Operation(summary = "Get All Collections (Paginated)",
            description = "Retrieves a paginated list of all collections with mobile-optimized pagination metadata. Requires authentication.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved collections")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @Transactional(readOnly = true)
    public ResponseEntity<PagedResponse<CollectionDto>> getAllCollections(Pageable pageable) {
        Page<CollectionDto> collectionDtos = collectionService.getAllCollections(pageable)
                .map(collection -> CollectionDto.fromEntity(collection, storageService));
        PagedResponse<CollectionDto> response = PagedResponse.from(collectionDtos);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Collection by ID",
            description = "Retrieves a specific collection by its ID. Requires authentication.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved collection")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Collection not found")
    @Transactional(readOnly = true)
    public ResponseEntity<CollectionDto> getCollectionById(
            @Parameter(description = "The ID of the collection") @PathVariable Long id) {
        return collectionService.getCollectionById(id)
                .map(collection -> ResponseEntity.ok(CollectionDto.fromEntity(collection, storageService)))
                .orElse(ResponseEntity.notFound().build());
    }
}