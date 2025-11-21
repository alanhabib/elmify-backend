package com.elmify.backend.config; // Assuming it stays in the 'config' package

import com.elmify.backend.security.ClerkJwtAuthenticationConverter;
import com.elmify.backend.security.ClerkJwtDecoder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Central security configuration for the application.
 * Enables JWT-based authentication for the API and configures CORS.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ClerkJwtDecoder clerkJwtDecoder;
    private final ClerkJwtAuthenticationConverter clerkJwtAuthenticationConverter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(content -> {})
                        .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                                .maxAgeInSeconds(31536000) // 1 year
                                .includeSubDomains(true)
                        )
                        .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                        .permissionsPolicy(permissions -> permissions
                                .policy("camera=(), microphone=(), geolocation=()")
                        )
                )
                .authorizeHttpRequests(authorize -> authorize
                        // Public health check endpoints
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        // Public Swagger/OpenAPI endpoints
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
                        // Public user sync endpoint (called during authentication)
                        .requestMatchers("/api/v1/users/sync").permitAll()
                        // Public audio streaming endpoints (for guest mode)
                        .requestMatchers("/api/v1/lectures/*/stream").permitAll()
                        .requestMatchers("/api/v1/lectures/*/stream-url").permitAll()
                        // Public browsing endpoints (speakers, collections, lectures)
                        .requestMatchers("/api/v1/speakers/**").permitAll()
                        .requestMatchers("/api/v1/collections/**").permitAll()
                        .requestMatchers("/api/v1/lectures/**").permitAll()
                        // All other API endpoints require authentication
                        .requestMatchers("/api/v1/**").authenticated()
                        // Deny all other requests
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(clerkJwtDecoder)
                                .jwtAuthenticationConverter(clerkJwtAuthenticationConverter)
                        )
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Authentication required\",\"message\":\"Valid JWT token is required to access this resource\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Access denied\",\"message\":\"Insufficient privileges to access this resource\"}");
                        })
                );

        return http.build();
    }

    /**
     * Configures CORS using a flexible, pattern-based approach suitable for development.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // This is the improved logic from your original CorsConfig.java
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://192.168.39.138:*",
                "exp://*" // For Expo Go development
        ));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*")); // Allow all headers
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}