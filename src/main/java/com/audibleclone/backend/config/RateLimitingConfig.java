package com.audibleclone.backend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limiting configuration for API endpoints
 */
@Configuration
@ConditionalOnProperty(name = "audibleclone.security.rate-limiting.enabled", havingValue = "true", matchIfMissing = false)
public class RateLimitingConfig {

    /**
     * Rate limiting filter that applies to all requests
     */
    @Component
    @Order(1)
    @Slf4j
    public static class RateLimitingFilter extends OncePerRequestFilter {

        private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                      FilterChain filterChain) throws ServletException, IOException {
            
            String clientIp = getClientIP(request);
            String endpoint = request.getRequestURI();
            
            // Different rate limits for different endpoint types
            Bucket bucket = getBucket(clientIp, endpoint);
            
            if (bucket.tryConsume(1)) {
                filterChain.doFilter(request, response);
            } else {
                log.warn("Rate limit exceeded for IP: {} on endpoint: {}", clientIp, endpoint);
                response.setStatus(429); // HTTP 429 Too Many Requests
                response.setContentType("application/json");
                response.getWriter().write(
                    "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\"}"
                );
            }
        }

        private Bucket getBucket(String clientIp, String endpoint) {
            String key = clientIp + ":" + getBucketType(endpoint);
            return buckets.computeIfAbsent(key, this::createBucket);
        }

        private String getBucketType(String endpoint) {
            if (endpoint.contains("/stream-url")) {
                return "STREAMING";  // More restrictive for streaming endpoints
            } else if (endpoint.contains("/api/v1/analytics")) {
                return "ANALYTICS";  // Moderate limits for analytics
            } else if (endpoint.contains("/api/v1/migration")) {
                return "ADMIN";      // Very restrictive for admin operations
            }
            return "GENERAL";        // Default rate limit
        }

        private Bucket createBucket(String key) {
            String bucketType = key.split(":")[1];
            
            return switch (bucketType) {
                case "STREAMING" -> Bucket4j.builder()
                    .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1))))
                    .build();
                case "ANALYTICS" -> Bucket4j.builder()
                    .addLimit(Bandwidth.classic(60, Refill.intervally(60, Duration.ofMinutes(1))))
                    .build();
                case "ADMIN" -> Bucket4j.builder()
                    .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1))))
                    .build();
                default -> Bucket4j.builder()
                    .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
                    .build();
            };
        }

        private String getClientIP(HttpServletRequest request) {
            String xfHeader = request.getHeader("X-Forwarded-For");
            if (xfHeader != null && !xfHeader.isEmpty() && !"unknown".equalsIgnoreCase(xfHeader)) {
                return xfHeader.split(",")[0].trim();
            }
            
            String xrealHeader = request.getHeader("X-Real-IP");
            if (xrealHeader != null && !xrealHeader.isEmpty() && !"unknown".equalsIgnoreCase(xrealHeader)) {
                return xrealHeader;
            }
            
            return request.getRemoteAddr();
        }
    }
}