package com.elmify.backend.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Converts a Clerk JWT into a Spring Security authentication token.
 *
 * <p>This converter extracts the user's identity (subject/clerkId) and any roles from the JWT
 * claims, creating an authentication token that Spring Security can use for authorization
 * decisions.
 */
@Component
@Slf4j
public class ClerkJwtAuthenticationConverter
    implements Converter<Jwt, AbstractAuthenticationToken> {

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    String subject = jwt.getSubject();

    if (log.isDebugEnabled()) {
      log.debug("Converting JWT for subject: {}", subject);
      log.debug("JWT claims: {}", jwt.getClaims().keySet());
    }

    Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

    log.debug("Authenticated user: {} with authorities: {}", subject, authorities);

    return new JwtAuthenticationToken(jwt, authorities, subject);
  }

  private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
    List<GrantedAuthority> authorities = new ArrayList<>();

    // Every authenticated user gets the USER role
    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

    // Check for admin role in Clerk metadata
    // Clerk can store roles in public_metadata or private_metadata
    Map<String, Object> publicMetadata = jwt.getClaim("public_metadata");
    if (publicMetadata != null) {
      Object role = publicMetadata.get("role");
      if ("admin".equals(role) || "ADMIN".equals(role)) {
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
      }

      // Check for roles array
      Object roles = publicMetadata.get("roles");
      if (roles instanceof List<?> rolesList) {
        for (Object r : rolesList) {
          if (r instanceof String roleStr) {
            String authority =
                roleStr.toUpperCase().startsWith("ROLE_")
                    ? roleStr.toUpperCase()
                    : "ROLE_" + roleStr.toUpperCase();
            authorities.add(new SimpleGrantedAuthority(authority));
          }
        }
      }
    }

    // Check for org_role claim (Clerk Organizations feature)
    String orgRole = jwt.getClaimAsString("org_role");
    if (orgRole != null && (orgRole.contains("admin") || orgRole.contains("owner"))) {
      authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    return authorities;
  }
}
