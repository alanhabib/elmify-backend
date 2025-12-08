package com.elmify.backend.security;

import com.elmify.backend.entity.User;
import com.elmify.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that automatically syncs users from Clerk JWT claims to the local database.
 *
 * <p>This filter runs AFTER the JWT authentication filter, so the SecurityContext is already
 * populated with the authenticated user's JWT.
 *
 * <p>When a user authenticates with a valid Clerk JWT: 1. Extract user info from JWT claims (sub,
 * email, name, picture) 2. Check if user exists in database 3. If not, create the user
 * automatically 4. If exists, update email/name if changed
 *
 * <p>This eliminates the need for the frontend to call /sync explicitly and ensures users are
 * always synced on first authenticated request.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1) // Run after Spring Security filters
public class JwtUserSyncFilter extends OncePerRequestFilter {

  private final UserRepository userRepository;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    try {
      syncUserFromJwtIfAuthenticated();
    } catch (Exception e) {
      // Don't fail the request if sync fails - just log it
      log.warn("Failed to sync user from JWT: {}", e.getMessage());
    }

    filterChain.doFilter(request, response);
  }

  private void syncUserFromJwtIfAuthenticated() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
      return; // Not JWT authentication, skip
    }

    Jwt jwt = jwtAuth.getToken();
    String clerkId = jwt.getSubject();

    if (clerkId == null || clerkId.isEmpty()) {
      log.warn("JWT missing subject claim");
      return;
    }

    // Check if user already exists
    if (userRepository.existsByClerkId(clerkId)) {
      // User exists, optionally update their info
      updateUserIfNeeded(clerkId, jwt);
      return;
    }

    // User doesn't exist - create them from JWT claims
    createUserFromJwt(clerkId, jwt);
  }

  private void createUserFromJwt(String clerkId, Jwt jwt) {
    String email = extractEmail(jwt);
    String displayName = extractDisplayName(jwt);
    String profileImageUrl = extractProfileImage(jwt);

    User newUser = new User();
    newUser.setClerkId(clerkId);
    newUser.setEmail(email);
    newUser.setDisplayName(displayName);
    newUser.setProfileImageUrl(profileImageUrl);

    userRepository.save(newUser);
    log.info("Auto-synced new user from JWT. ClerkId: {}, Email: {}", clerkId, email);
  }

  private void updateUserIfNeeded(String clerkId, Jwt jwt) {
    // Only update if email changed (optional optimization)
    String jwtEmail = extractEmail(jwt);

    userRepository
        .findByClerkId(clerkId)
        .ifPresent(
            user -> {
              boolean updated = false;

              // Update email if changed
              if (jwtEmail != null && !jwtEmail.equals(user.getEmail())) {
                user.setEmail(jwtEmail);
                updated = true;
              }

              // Update display name if changed
              String jwtName = extractDisplayName(jwt);
              if (jwtName != null && !jwtName.equals(user.getDisplayName())) {
                user.setDisplayName(jwtName);
                updated = true;
              }

              // Update profile image if changed
              String jwtImage = extractProfileImage(jwt);
              if (jwtImage != null && !jwtImage.equals(user.getProfileImageUrl())) {
                user.setProfileImageUrl(jwtImage);
                updated = true;
              }

              if (updated) {
                userRepository.save(user);
                log.debug("Updated user info from JWT for ClerkId: {}", clerkId);
              }
            });
  }

  /**
   * Extract email from Clerk JWT. Clerk puts email in different claims depending on configuration.
   */
  private String extractEmail(Jwt jwt) {
    // Try standard claims first
    String email = jwt.getClaimAsString("email");
    if (email != null) {
      return email;
    }

    // Clerk sometimes uses 'primary_email' or nested structure
    email = jwt.getClaimAsString("primary_email");
    if (email != null) {
      return email;
    }

    // Check for email_addresses array (Clerk format)
    Object emailAddresses = jwt.getClaim("email_addresses");
    if (emailAddresses instanceof java.util.List<?> list && !list.isEmpty()) {
      Object first = list.get(0);
      if (first instanceof java.util.Map<?, ?> map) {
        Object emailValue = map.get("email_address");
        if (emailValue instanceof String) {
          return (String) emailValue;
        }
      }
    }

    return null;
  }

  /** Extract display name from Clerk JWT. */
  private String extractDisplayName(Jwt jwt) {
    // Try different possible claim names
    String name = jwt.getClaimAsString("name");
    if (name != null && !name.isEmpty()) {
      return name;
    }

    // Try first_name + last_name
    String firstName = jwt.getClaimAsString("first_name");
    String lastName = jwt.getClaimAsString("last_name");

    if (firstName != null || lastName != null) {
      StringBuilder sb = new StringBuilder();
      if (firstName != null) sb.append(firstName);
      if (lastName != null) {
        if (sb.length() > 0) sb.append(" ");
        sb.append(lastName);
      }
      return sb.toString();
    }

    // Try username
    String username = jwt.getClaimAsString("username");
    if (username != null) {
      return username;
    }

    // Fallback to part of email
    String email = extractEmail(jwt);
    if (email != null && email.contains("@")) {
      return email.substring(0, email.indexOf("@"));
    }

    return null;
  }

  /** Extract profile image URL from Clerk JWT. */
  private String extractProfileImage(Jwt jwt) {
    // Clerk uses 'image_url' or 'profile_image_url' or 'picture'
    String image = jwt.getClaimAsString("image_url");
    if (image != null) {
      return image;
    }

    image = jwt.getClaimAsString("profile_image_url");
    if (image != null) {
      return image;
    }

    image = jwt.getClaimAsString("picture");
    if (image != null) {
      return image;
    }

    return null;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    // Skip for health checks and actuator endpoints
    String path = request.getRequestURI();
    return path.startsWith("/actuator")
        || path.equals("/health")
        || path.startsWith("/swagger")
        || path.startsWith("/v3/api-docs");
  }
}
