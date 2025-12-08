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

import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

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
    log.info("Configuring security filter chain for profile: {}", activeProfile);

    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .anonymous(anonymous -> anonymous.principal("guest").authorities("ROLE_ANONYMOUS"))
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
              var auth =
                  authorize
                      // Public health check endpoints
                      .requestMatchers("/actuator/health", "/actuator/health/**")
                      .permitAll();

              // Swagger only in development
              if ("dev".equals(activeProfile)) {
                auth =
                    auth.requestMatchers(
                            "/swagger-ui.html",
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/swagger-resources/**",
                            "/webjars/**")
                        .permitAll();
              }

              auth
                  // ===== PUBLIC GET ENDPOINTS (for browsing/streaming) =====
                  .requestMatchers(HttpMethod.GET, "/api/v1/speakers/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/v1/collections/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/v1/lectures/**")
                  .permitAll();

              auth
                  // ===== PUBLIC POST ENDPOINTS =====
                  // Playlist manifest endpoint is public (for guest access)
                  .requestMatchers(HttpMethod.POST, "/api/v1/playlists/manifest")
                  .permitAll()
                  // ===== AUTHENTICATED ENDPOINTS =====
                  // Playback tracking requires authentication
                  .requestMatchers("/api/v1/playback/**")
                  .authenticated()
                  // User endpoints (except sync) require authentication
                  .requestMatchers("/api/v1/users/me")
                  .authenticated()
                  // Favorites require authentication
                  .requestMatchers("/api/v1/favorites/**")
                  .authenticated()
                  // All OTHER POST/PUT/PATCH/DELETE operations require authentication
                  .requestMatchers(HttpMethod.POST, "/api/v1/**")
                  .authenticated()
                  .requestMatchers(HttpMethod.PUT, "/api/v1/**")
                  .authenticated()
                  .requestMatchers(HttpMethod.PATCH, "/api/v1/**")
                  .authenticated()
                  .requestMatchers(HttpMethod.DELETE, "/api/v1/**")
                  .authenticated()
                  // Allow any other GET requests (public browsing)
                  .requestMatchers(HttpMethod.GET, "/api/v1/**")
                  .permitAll()
                  // Deny all other requests (non-API paths)
                  .anyRequest()
                  .denyAll();
            })
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .jwt(
                        jwt ->
                            jwt.decoder(clerkJwtDecoder)
                                .jwtAuthenticationConverter(clerkJwtAuthenticationConverter))
                    .authenticationEntryPoint(
                        (request, response, authException) -> {
                          // For protected endpoints, require authentication
                          log.warn(
                              "Authentication required: {} {} from IP: {}",
                              request.getMethod(),
                              request.getRequestURI(),
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

    // Check if using wildcard
    // NOTE: Wildcard CORS (*) is configured for React Native mobile app.
    // React Native apps don't enforce CORS (not browser-based), so wildcard is safe.
    // If web clients are added in the future, configure specific origins instead.
    boolean isWildcard = origins.contains("*") || allowedOriginsConfig.equals("*");

    if (isWildcard) {
      log.warn("CORS: Allowing ALL origins (*) - React Native app (CORS not enforced by native apps)");
      configuration.setAllowedOriginPatterns(List.of("*"));
      configuration.setAllowCredentials(false);
    } else {
      log.info("CORS: Configured {} allowed origins", origins.size());
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
