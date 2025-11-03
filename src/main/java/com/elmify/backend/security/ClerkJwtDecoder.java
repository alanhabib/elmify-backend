package com.elmify.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

/**
 * A production-grade JwtDecoder to validate JWTs issued by Clerk.
 * This implementation uses Spring Security's standard, robust components to
 * fetch the JWKS (JSON Web Key Set) and validate the token's signature and claims.
 */
@Component
public class ClerkJwtDecoder implements JwtDecoder {

    private static final Logger logger = LoggerFactory.getLogger(ClerkJwtDecoder.class);

    private final NimbusJwtDecoder jwtDecoder;

    public ClerkJwtDecoder(@Value("${elmify.clerk.jwt-issuer}") String clerkIssuer) {
        this.jwtDecoder = createJwtDecoder(clerkIssuer);
    }

    /**
     * Creates a NimbusJwtDecoder configured for Clerk using standard Spring Security components.
     * It handles fetching, caching, and refreshing the JWKS keys automatically and gracefully.
     *
     * @param issuer The expected 'iss' claim in the JWT, which is also used to locate the JWKS URI.
     * @return A fully configured NimbusJwtDecoder.
     */
    private NimbusJwtDecoder createJwtDecoder(String issuer) {
        try {
            logger.info("Creating JWT decoder for issuer: {}", issuer);
            
            // Use the standard Spring Security validator that checks the issuer, expiration (exp),
            // and not-before (nbf) claims.
            OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
            OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withIssuer);

            // Try using the issuer location first (auto-discovery)
            NimbusJwtDecoder decoder;
            try {
                logger.info("Attempting auto-discovery for issuer: {}", issuer);
                decoder = JwtDecoders.fromIssuerLocation(issuer);
            } catch (Exception e) {
                logger.warn("Auto-discovery failed for issuer: {}, trying manual JWKS URI", issuer, e);
                // Fallback to manual JWKS URI
                String jwksUri = issuer + "/.well-known/jwks.json";
                logger.info("Using manual JWKS URI: {}", jwksUri);
                decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
            }
            
            decoder.setJwtValidator(validator);
            logger.info("Successfully configured JWT decoder for issuer: {}", issuer);
            
            return decoder;
        } catch (Exception e) {
            logger.error("Failed to create JWT decoder for issuer: {}", issuer, e);
            throw new IllegalStateException("Unable to configure JWT decoder for Clerk", e);
        }
    }

    /**
     * Decodes and validates the provided JWT string.
     *
     * @param token The JWT string to decode.
     * @return A validated Jwt object.
     * @throws JwtException if the token is invalid for any reason (bad signature, expired, wrong issuer, etc.).
     */
    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            return jwtDecoder.decode(token);
        } catch (JwtException e) {
            // Log the specific validation error for easier debugging in logs.
            logger.debug("JWT validation failed: {}", e.getMessage());
            // Re-throw the exception to let the Spring Security filter chain handle the authentication failure.
            throw e;
        }
    }
}