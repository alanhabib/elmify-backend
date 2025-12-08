package com.elmify.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

/**
 * A production-grade JwtDecoder to validate JWTs issued by Clerk. This implementation uses Spring
 * Security's standard, robust components to fetch the JWKS (JSON Web Key Set) and validate the
 * token's signature and claims.
 */
@Component
public class ClerkJwtDecoder implements JwtDecoder {

  private static final Logger logger = LoggerFactory.getLogger(ClerkJwtDecoder.class);

  private final NimbusJwtDecoder jwtDecoder;
  private final String expectedIssuer;

  public ClerkJwtDecoder(@Value("${elmify.clerk.jwt-issuer}") String clerkIssuer) {
    this.expectedIssuer = clerkIssuer;
    this.jwtDecoder = createJwtDecoder(clerkIssuer);
    logger.info("✅ ClerkJwtDecoder initialized. Expected JWT issuer: {}", clerkIssuer);
  }

  /**
   * Creates a NimbusJwtDecoder configured for Clerk using standard Spring Security components. It
   * handles fetching, caching, and refreshing the JWKS keys automatically and gracefully.
   *
   * @param issuer The expected 'iss' claim in the JWT, which is also used to locate the JWKS URI.
   * @return A fully configured NimbusJwtDecoder.
   */
  private NimbusJwtDecoder createJwtDecoder(String issuer) {
    logger.info("🔐 Creating JWT decoder for issuer: {}", issuer);

    // Validate issuer URL format
    if (issuer == null || issuer.isBlank()) {
      throw new IllegalStateException(
          "CLERK_JWT_ISSUER is not configured! Set the CLERK_JWT_ISSUER environment variable.");
    }

    if (!issuer.startsWith("https://")) {
      logger.warn("⚠️ Issuer URL should start with https:// - current value: {}", issuer);
    }

    try {
      // Use the standard Spring Security validator that checks the issuer, expiration (exp),
      // and not-before (nbf) claims.
      OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
      OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withIssuer);

      // Build the JWKS URI
      String jwksUri = issuer + "/.well-known/jwks.json";
      logger.info("📡 JWKS URI that will be used: {}", jwksUri);

      // Create decoder with manual JWKS URI (more reliable than auto-discovery)
      NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
      decoder.setJwtValidator(validator);

      logger.info("✅ JWT decoder configured successfully for issuer: {}", issuer);
      logger.info(
          "💡 If you see 'UnknownHostException', verify CLERK_JWT_ISSUER is correct. "
              + "It should match your Clerk instance URL (e.g., https://your-app.clerk.accounts.dev)");

      return decoder;
    } catch (Exception e) {
      logger.error(
          "❌ Failed to create JWT decoder for issuer: {} - Error: {}", issuer, e.getMessage());
      logger.error("💡 Common causes:");
      logger.error(
          "   1. CLERK_JWT_ISSUER is set to a non-existent domain (e.g., custom domain not configured)");
      logger.error("   2. DNS cannot resolve the issuer hostname");
      logger.error("   3. Network connectivity issues from Railway to Clerk");
      logger.error(
          "   Recommended: Set CLERK_JWT_ISSUER to your actual Clerk URL like https://your-app.clerk.accounts.dev");
      throw new IllegalStateException(
          "Unable to configure JWT decoder for Clerk. Check CLERK_JWT_ISSUER value: " + issuer, e);
    }
  }

  /**
   * Decodes and validates the provided JWT string.
   *
   * @param token The JWT string to decode.
   * @return A validated Jwt object.
   * @throws JwtException if the token is invalid for any reason (bad signature, expired, wrong
   *     issuer, etc.).
   */
  @Override
  public Jwt decode(String token) throws JwtException {
    try {
      Jwt jwt = jwtDecoder.decode(token);

      logger.info(
          "JWT decoded successfully. Subject: {}, Issuer: {}, Expiry: {}",
          jwt.getSubject(),
          jwt.getIssuer(),
          jwt.getExpiresAt());

      return jwt;
    } catch (JwtException e) {
      // Log detailed validation error for easier debugging
      // Try to extract issuer from token for debugging (without validation)
      String tokenIssuer = extractIssuerFromToken(token);
      logger.error(
          "❌ JWT validation failed! Error: {} | Token issuer: '{}' | Expected issuer: '{}'",
          e.getMessage(),
          tokenIssuer,
          expectedIssuer);
      throw e;
    }
  }

  /** Extract issuer from JWT token without validation (for debugging purposes only) */
  private String extractIssuerFromToken(String token) {
    try {
      if (token == null || !token.contains(".")) {
        return "invalid-token-format";
      }
      String[] parts = token.split("\\.");
      if (parts.length < 2) {
        return "invalid-token-format";
      }
      String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
      // Simple extraction - look for "iss" claim
      int issIndex = payload.indexOf("\"iss\"");
      if (issIndex == -1) {
        return "no-iss-claim";
      }
      int colonIndex = payload.indexOf(":", issIndex);
      int quoteStart = payload.indexOf("\"", colonIndex);
      int quoteEnd = payload.indexOf("\"", quoteStart + 1);
      if (quoteStart != -1 && quoteEnd != -1) {
        return payload.substring(quoteStart + 1, quoteEnd);
      }
      return "could-not-parse";
    } catch (Exception ex) {
      return "parse-error: " + ex.getMessage();
    }
  }
}
