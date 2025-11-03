package com.elmify.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

public final class SecurityUtils {
    
    private SecurityUtils() {
        // Utility class
    }
    
    /**
     * Get the current authenticated user's ID from the JWT token
     */
    public static Optional<String> getCurrentUserId() {
        return getCurrentJwt()
            .map(jwt -> jwt.getClaimAsString("sub"));
    }
    
    /**
     * Get the current authenticated user's email from the JWT token
     */
    public static Optional<String> getCurrentUserEmail() {
        return getCurrentJwt()
            .map(jwt -> jwt.getClaimAsString("email"));
    }
    
    /**
     * Get the current JWT token
     */
    public static Optional<Jwt> getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return Optional.of(jwtAuth.getToken());
        }
        
        return Optional.empty();
    }
    
    /**
     * Check if current user is authenticated
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && 
               authentication.isAuthenticated() && 
               authentication instanceof JwtAuthenticationToken;
    }
    
    /**
     * Check if current user has a specific role
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals(roleWithPrefix));
    }
    
    /**
     * Check if current user is an admin
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }
}