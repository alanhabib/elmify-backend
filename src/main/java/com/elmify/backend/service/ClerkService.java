package com.elmify.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Service for interacting with Clerk Backend API.
 * Used for operations that require server-side API access like deleting users.
 */
@Service
@Slf4j
public class ClerkService {

    private final RestTemplate restTemplate;
    private final String secretKey;
    private static final String CLERK_API_BASE = "https://api.clerk.com/v1";

    public ClerkService(
            @Value("${elmify.clerk.secret-key}") String secretKey,
            RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(30))
            .build();
        this.secretKey = secretKey;
    }

    /**
     * Delete a user from Clerk.
     * This permanently removes the user's authentication account.
     *
     * @param clerkUserId The Clerk user ID (e.g., "user_abc123")
     * @throws RuntimeException if deletion fails
     */
    public void deleteUser(String clerkUserId) {
        String url = CLERK_API_BASE + "/users/" + clerkUserId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(secretKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully deleted Clerk user: {}", clerkUserId);
            } else {
                log.error("Unexpected response deleting Clerk user {}: {}", clerkUserId, response.getStatusCode());
                throw new RuntimeException("Failed to delete Clerk user: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException.NotFound e) {
            // User already doesn't exist in Clerk - that's fine
            log.warn("Clerk user not found (may already be deleted): {}", clerkUserId);
        } catch (HttpClientErrorException e) {
            log.error("Error deleting Clerk user {}: {} - {}", clerkUserId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to delete Clerk user: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error deleting Clerk user {}: {}", clerkUserId, e.getMessage());
            throw new RuntimeException("Failed to delete Clerk user: " + e.getMessage());
        }
    }
}
