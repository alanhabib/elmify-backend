package com.elmify.backend.config; // Assuming it stays in the 'config' package

import com.elmify.backend.security.ClerkJwtAuthenticationConverter;
import com.elmify.backend.security.ClerkJwtDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Central security configuration for the application. Enables JWT-based authentication for the API
 * and configures CORS.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

  private final ClerkJwtDecoder clerkJwtDecoder;
  private final ClerkJwtAuthenticationConverter clerkJwtAuthenticationConverter;

  @Value("${elmify.cors.allowed-origins}")
  private String allowedOriginsConfig;

  @Value("${spring.profiles.active:dev}")
  private String activeProfile;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    log.info("🔒 Configuring security with profile: {}", activeProfile);
    log.info("🌐 CORS allowed origins: {}", allowedOriginsConfig);

    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(
            headers ->
                headers
                    .frameOptions(frame -> frame.deny())
                    .contentTypeOptions(content -> {})
                    .contentSecurityPolicy(
                        csp ->
                            csp.policyDirectives(
                                "default-src 'self'; connect-src 'self' https://cdn.elmify.store; img-src 'self' data: https:; style-src 'self' 'unsafe-inline'"))
                    .httpStrictTransportSecurity(
                        hstsConfig ->
                            hstsConfig
                                .maxAgeInSeconds(31536000) // 1 year
                                .includeSubDomains(true)
                                .preload(true))
                    .referrerPolicy(
                        referrer ->
                            referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                    .permissionsPolicy(
                        permissions ->
                            permissions.policy("camera=(), microphone=(), geolocation=()")))
        .authorizeHttpRequests(
            authorize -> {
              log.info("🔒 Configuring HTTP request authorization...");

              var auth =
                  authorize
                      // Public health check endpoints
                      .requestMatchers("/actuator/health", "/actuator/health/**")
                      .permitAll();

              // Swagger only in development
              if ("dev".equals(activeProfile)) {
                log.info("📚 Enabling Swagger for development profile");
                auth =
                    auth.requestMatchers(
                            "/swagger-ui.html",
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/swagger-resources/**",
                            "/webjars/**")
                        .permitAll();
              } else {
                log.info("📚 Swagger DISABLED for profile: {}", activeProfile);
              }

              auth
                  // Public user sync endpoint (called during authentication)
                  .requestMatchers("/api/v1/users/sync")
                  .permitAll()

                  // ===== PUBLIC GET ENDPOINTS (for browsing/streaming) =====
                  // Allow public GET access for browsing content
                  .requestMatchers(HttpMethod.GET, "/api/v1/speakers/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/v1/collections/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/v1/lectures/**")
                  .permitAll();

              log.info("✅ Public GET endpoints configured: /speakers, /collections, /lectures");

              auth
                  // ===== AUTHENTICATED ENDPOINTS (modifications) =====
                  // All POST/PUT/PATCH/DELETE operations require authentication
                  .requestMatchers(HttpMethod.POST, "/api/v1/**")
                  .authenticated()
                  .requestMatchers(HttpMethod.PUT, "/api/v1/**")
                  .authenticated()
                  .requestMatchers(HttpMethod.PATCH, "/api/v1/**")
                  .authenticated()
                  .requestMatchers(HttpMethod.DELETE, "/api/v1/**")
                  .authenticated()

                  // All other API endpoints require authentication
                  .requestMatchers("/api/v1/**")
                  .authenticated()
                  // Deny all other requests
                  .anyRequest()
                  .denyAll();

              log.info("✅ Security configuration complete");
            })
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .jwt(
                        jwt ->
                            jwt.decoder(clerkJwtDecoder)
                                .jwtAuthenticationConverter(clerkJwtAuthenticationConverter))
                    // Make JWT authentication optional - don't reject requests without valid tokens
                    .authenticationEntryPoint(
                        (request, response, authException) -> {
                          // For public endpoints, treat missing/invalid JWT as anonymous user
                          // This allows permitAll() to work properly
                          String uri = request.getRequestURI();
                          if (uri.startsWith("/api/v1/speakers")
                              || uri.startsWith("/api/v1/collections")
                              || uri.startsWith("/api/v1/lectures")
                              || uri.startsWith("/actuator/health")
                              || uri.startsWith("/api/v1/users/sync")) {
                            // Public endpoint - allow through without authentication
                            log.debug("Public endpoint accessed without JWT: {}", uri);
                            response.setStatus(200); // Will be handled by the actual controller
                            return;
                          }

                          // For protected endpoints, require authentication
                          log.warn(
                              "Authentication failed: {} {} from IP: {}",
                              request.getMethod(),
                              uri,
                              request.getRemoteAddr());

                          response.setStatus(401);
                          response.setContentType("application/json");
                          response
                              .getWriter()
                              .write(
                                  "{\"error\":\"Authentication required\",\"message\":\"Valid JWT token is required to access this resource\"}");
                        }))
        .exceptionHandling(
            exceptions ->
                exceptions.accessDeniedHandler(
                    (request, response, accessDeniedException) -> {
                      log.warn(
                          "Access denied: {} {} from IP: {}",
                          request.getMethod(),
                          request.getRequestURI(),
                          request.getRemoteAddr());

                      response.setStatus(403);
                      response.setContentType("application/json");
                      response
                          .getWriter()
                          .write(
                              "{\"error\":\"Access denied\",\"message\":\"Insufficient privileges to access this resource\"}");
                    }));

    return http.build();
  }

  /** Configures CORS using environment-specific origins for security. */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // Parse comma-separated origins from environment variable
    List<String> origins = Arrays.asList(allowedOriginsConfig.split(","));

    // Check if using wildcard (temporary for testing)
    boolean isWildcard = origins.contains("*") || allowedOriginsConfig.equals("*");

    if (isWildcard) {
      // Wildcard mode: allow all origins (TESTING ONLY)
      log.warn("⚠️ CORS: Allowing ALL origins (*) - This is NOT secure for production!");
      configuration.setAllowedOriginPatterns(List.of("*"));
      configuration.setAllowCredentials(false); // Cannot use credentials with wildcard
    } else {
      // Production mode: specific origins
      log.info("CORS: Allowing specific origins: {}", origins);
      configuration.setAllowedOriginPatterns(origins);
      configuration.setAllowCredentials(true);
    }

    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*")); // Allow all headers
    configuration.setMaxAge(3600L); // Cache preflight for 1 hour

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
