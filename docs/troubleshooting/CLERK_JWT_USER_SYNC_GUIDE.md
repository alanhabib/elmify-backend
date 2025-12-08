# Clerk JWT Authentication & User Sync Guide

> **Date:** December 8, 2025  
> **Issue:** Backend JWT verification failing, users not syncing on login  
> **Status:** ✅ RESOLVED

---

## Problem Summary

When users logged in via Clerk from the React Native (Expo) frontend, the backend was failing to:

1. Properly validate JWT tokens
2. Automatically sync user profiles to the PostgreSQL database
3. Allow access to `/api/v1/users/me` endpoint

### Error Logs Observed

```
WARN  c.e.backend.config.SecurityConfig - Authentication failed: GET /api/v1/users/me from IP: x.x.x.x
WARN  c.e.b.e.GlobalExceptionHandler - Access denied: Access Denied [traceId: xxxxx]
```

---

## Root Cause Analysis

### 1. Missing Automatic User Sync

The original implementation required the frontend to explicitly call `/api/v1/users/sync` after login. However:

- The sync endpoint required `@PreAuthorize("isAuthenticated()")`
- If the JWT validation failed for any reason, users couldn't sync
- If the user didn't exist in the database, `/api/v1/users/me` returned 404

### 2. JWT Validation Issues

The Clerk JWT decoder was not logging validation failures clearly, making debugging difficult.

### 3. Missing User Creation on First Access

There was no mechanism to automatically create users when they first authenticated with a valid JWT.

---

## Solution Implemented

### 1. Created `JwtUserSyncFilter`

A new filter that runs AFTER JWT authentication to automatically sync users from JWT claims:

**File:** `src/main/java/com/elmify/backend/security/JwtUserSyncFilter.java`

```java
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class JwtUserSyncFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            syncUserFromJwtIfAuthenticated();
        } catch (Exception e) {
            log.warn("Failed to sync user from JWT: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
    
    // ... extracts user info from JWT claims (sub, email, name, picture)
    // ... creates or updates user in database
}
```

**Key Features:**

- Extracts `sub` (clerkId), `email`, `name`/`first_name`+`last_name`, and `picture`/`image_url` from JWT
- Creates new users automatically on first authentication
- Updates existing users if their info changed
- Handles various Clerk JWT claim formats

### 2. Updated SecurityConfig

Added the filter to the security filter chain:

```java
.addFilterAfter(jwtUserSyncFilter, BearerTokenAuthenticationFilter.class);
```

### 3. Improved JWT Decoder Logging

Enhanced `ClerkJwtDecoder` to log validation details:

```java
@Override
public Jwt decode(String token) throws JwtException {
    try {
        Jwt jwt = jwtDecoder.decode(token);
        if (logger.isDebugEnabled()) {
            logger.debug("JWT decoded successfully. Subject: {}, Issuer: {}, Expiry: {}", 
                jwt.getSubject(), jwt.getIssuer(), jwt.getExpiresAt());
        }
        return jwt;
    } catch (JwtException e) {
        logger.warn("JWT validation failed: {} - Token prefix: {}...", 
            e.getMessage(), token != null && token.length() > 20 ? token.substring(0, 20) : "null/short");
        throw e;
    }
}
```

### 4. Improved JWT Authentication Converter

Enhanced `ClerkJwtAuthenticationConverter` to extract roles from Clerk metadata:

```java
private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
    List<GrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    
    // Check for admin role in Clerk public_metadata
    Map<String, Object> publicMetadata = jwt.getClaim("public_metadata");
    if (publicMetadata != null) {
        Object role = publicMetadata.get("role");
        if ("admin".equals(role) || "ADMIN".equals(role)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
    }
    
    return authorities;
}
```

---

## Configuration Requirements

### Environment Variables

Ensure these are set in your environment (Railway, local `.env`, etc.):

```bash
# Clerk Configuration
CLERK_SECRET_KEY=sk_live_xxx  # or sk_test_xxx for development
CLERK_PUBLISHABLE_KEY=pk_live_xxx  # or pk_test_xxx for development
CLERK_JWT_ISSUER=https://clerk.elmify.store  # Your Clerk frontend API URL
```

### application-prod.yml

```yaml
elmify:
  clerk:
    secret-key: ${CLERK_SECRET_KEY}
    publishable-key: ${CLERK_PUBLISHABLE_KEY}
    jwt-issuer: ${CLERK_JWT_ISSUER}
```

---

## Authentication Flow (After Fix)

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   React Native  │    │  Spring Boot    │    │   PostgreSQL    │
│      (Expo)     │    │    Backend      │    │                 │
└────────┬────────┘    └────────┬────────┘    └────────┬────────┘
         │                      │                      │
         │  1. Login via Clerk  │                      │
         │  (User gets JWT)     │                      │
         │                      │                      │
         │  2. API Request      │                      │
         │  Authorization:      │                      │
         │  Bearer <JWT>        │                      │
         │─────────────────────>│                      │
         │                      │                      │
         │                      │  3. JWT Validated    │
         │                      │  by ClerkJwtDecoder  │
         │                      │                      │
         │                      │  4. JwtUserSyncFilter│
         │                      │  extracts user info  │
         │                      │  from JWT claims     │
         │                      │                      │
         │                      │  5. Check if user    │
         │                      │  exists in DB        │
         │                      │─────────────────────>│
         │                      │                      │
         │                      │  6. Create/Update    │
         │                      │  user if needed      │
         │                      │<─────────────────────│
         │                      │                      │
         │                      │  7. Process request  │
         │                      │  (e.g., /users/me)   │
         │                      │                      │
         │  8. Response         │                      │
         │<─────────────────────│                      │
         │                      │                      │
```

---

## Clerk JWT Claims Structure

Clerk JWTs typically include these claims:

```json
{
  "sub": "user_2abc123xyz",           // Clerk User ID (used as clerkId)
  "email": "user@example.com",
  "name": "John Doe",                 // or first_name/last_name
  "picture": "https://...",           // or image_url
  "iss": "https://clerk.elmify.store",
  "exp": 1733698800,
  "iat": 1733695200,
  "public_metadata": {                // Custom metadata set in Clerk
    "role": "admin"                   // For admin users
  }
}
```

---

## Troubleshooting

### JWT Validation Fails

1. Check `CLERK_JWT_ISSUER` matches your Clerk Frontend API URL
2. Ensure the JWT is not expired
3. Check logs for specific validation error messages

### User Not Found After Login

1. Verify the JWT contains the `sub` claim
2. Check logs for "Auto-synced new user from JWT" message
3. Ensure database is accessible

### 403 Forbidden on /users/me

1. Verify the JWT is being sent in `Authorization: Bearer <token>` header
2. Check that the endpoint requires authentication (not public)
3. Look for "Authentication required" warnings in logs

### Debug Mode

Enable debug logging in development:

```yaml
logging:
  level:
    com.elmify.backend.security: DEBUG
    org.springframework.security: DEBUG
```

---

## Files Modified

1. **NEW:** `src/main/java/com/elmify/backend/security/JwtUserSyncFilter.java`
2. **MODIFIED:** `src/main/java/com/elmify/backend/config/SecurityConfig.java`
3. **MODIFIED:** `src/main/java/com/elmify/backend/security/ClerkJwtAuthenticationConverter.java`
4. **MODIFIED:** `src/main/java/com/elmify/backend/security/ClerkJwtDecoder.java`
5. **MODIFIED:** `src/main/java/com/elmify/backend/controller/UserController.java`

---

## Testing

### Test Authentication Locally

```bash
# Get a test JWT from Clerk (you can copy from browser dev tools after login)
export TEST_JWT="eyJhbGciOiJSUzI1NiIs..."

# Test /users/me endpoint
curl -H "Authorization: Bearer $TEST_JWT" \
  http://localhost:8081/api/v1/users/me

# Expected: User profile JSON (auto-created if first time)
```

### Test in Production

```bash
curl -H "Authorization: Bearer $TEST_JWT" \
  https://elmify-backend-production.up.railway.app/api/v1/users/me
```

---

## Related Documentation

- [Understanding Spring Security JWT](../learning/understanding-spring-security-jwt.md)
- [Clerk Documentation](https://clerk.com/docs)
- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)

