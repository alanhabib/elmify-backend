# Understanding Spring Security & JWT Authentication

## Table of Contents
1. [What is Authentication?](#what-is-authentication)
2. [What is JWT?](#what-is-jwt)
3. [What is Clerk?](#what-is-clerk)
4. [How JWT Authentication Works](#how-jwt-authentication-works)
5. [Your Application's Security Flow](#your-applications-security-flow)
6. [Understanding SecurityConfig](#understanding-securityconfig)
7. [JWT Validation with ClerkJwtDecoder](#jwt-validation-with-clerkjwtdecoder)
8. [JWKS and Public Key Cryptography](#jwks-and-public-key-cryptography)
9. [Authorization with Roles](#authorization-with-roles)
10. [CORS Configuration](#cors-configuration)
11. [Security Headers](#security-headers)
12. [Common Security Patterns](#common-security-patterns)
13. [Troubleshooting](#troubleshooting)

---

## What is Authentication?

**Authentication** is proving **who you are**.

### Real-World Analogy
- **Authentication** = Showing your ID at the airport → "I am John Smith"
- **Authorization** = TSA checking if you have a boarding pass → "John Smith is allowed on Flight 123"

### In Your Application
- **Authentication**: User logs in with Clerk → Gets a JWT token → Backend verifies the token
- **Authorization**: Backend checks if user can access specific resources (e.g., admin-only endpoints)

---

## What is JWT?

**JWT** stands for **JSON Web Token**. It's a secure way to transmit information between parties.

### Structure of a JWT

A JWT has three parts separated by dots (`.`):

```
eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyXzEyMyIsImVtYWlsIjoiYWxhbkBleGFtcGxlLmNvbSIsImV4cCI6MTcwMDAwMDAwMH0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
        [HEADER]                            [PAYLOAD]                                                      [SIGNATURE]
```

### 1. Header (Algorithm & Token Type)
```json
{
  "alg": "RS256",        // Algorithm used to sign the token
  "typ": "JWT",          // Type of token
  "kid": "key-id-123"    // Key ID (which public key to use for verification)
}
```

### 2. Payload (Claims - The Actual Data)
```json
{
  "sub": "user_30sZ4mdRbYN9ALg2z7jy9lVs2PM",  // Subject (user ID from Clerk)
  "email": "dalanhabib@gmail.com",
  "iss": "https://strong-bison-1.clerk.accounts.dev",  // Issuer (who created the token)
  "aud": "https://api.example.com",                     // Audience (who should accept it)
  "exp": 1700000000,                                    // Expiration timestamp
  "iat": 1699999000,                                    // Issued at timestamp
  "nbf": 1699999000                                     // Not before timestamp
}
```

### 3. Signature (Proof of Authenticity)
```
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret_or_private_key
)
```

### Why JWT is Secure

1. **Signed**: The signature proves the token wasn't tampered with
2. **Stateless**: Backend doesn't need to store sessions (scalable!)
3. **Self-contained**: Token contains all info needed (user ID, email, etc.)
4. **Expirable**: Tokens expire automatically (security)

### JWT vs Session Cookies

| Feature | JWT | Session Cookie |
|---------|-----|----------------|
| Storage | Client-side (localStorage/memory) | Server-side (database/Redis) |
| Scalability | ✅ Excellent (stateless) | ❌ Harder (needs shared session store) |
| Expiration | Built-in (exp claim) | Manual (server deletes session) |
| Revocation | ❌ Hard (must wait for expiration) | ✅ Easy (delete session) |
| Mobile-friendly | ✅ Yes | ⚠️ Requires cookie support |

**Your app uses JWT** because it's perfect for mobile apps (React Native) and microservices!

---

## What is Clerk?

**Clerk** is a **third-party authentication service** that handles:
- User sign-up/sign-in
- Password management
- Social logins (Google, GitHub, etc.)
- Multi-factor authentication (MFA)
- JWT token generation

### Why Use Clerk?

**Without Clerk (DIY Authentication):**
```java
// You'd need to build ALL of this yourself:
- Password hashing (bcrypt, argon2)
- Password reset flows
- Email verification
- Rate limiting for login attempts
- OAuth integration (Google, Facebook, etc.)
- Session management
- JWT token generation and rotation
- Security best practices
```

**With Clerk:**
```javascript
// Frontend (React Native)
import { useAuth } from "@clerk/clerk-expo";

const { getToken } = useAuth();
const token = await getToken();

// Send token to backend - that's it! ✅
```

**Your backend only needs to:**
1. Validate the JWT token from Clerk
2. Extract user information (clerkId, email)
3. Check if user is authorized

Clerk handles **everything else**!

---

## How JWT Authentication Works

### The Complete Flow

```
┌─────────────┐                  ┌─────────────┐                 ┌──────────────┐
│   React     │                  │    Clerk    │                 │   Backend    │
│   Native    │                  │   Service   │                 │   (Spring)   │
│   App       │                  │             │                 │              │
└──────┬──────┘                  └──────┬──────┘                 └──────┬───────┘
       │                                │                                │
       │  1. User clicks "Login"        │                                │
       ├───────────────────────────────>│                                │
       │                                │                                │
       │  2. Clerk shows login UI       │                                │
       │     (email/password)           │                                │
       │<───────────────────────────────┤                                │
       │                                │                                │
       │  3. User enters credentials    │                                │
       ├───────────────────────────────>│                                │
       │                                │                                │
       │  4. Clerk validates & creates  │                                │
       │     JWT token                  │                                │
       │<───────────────────────────────┤                                │
       │                                │                                │
       │  5. App stores token           │                                │
       │                                │                                │
       │  6. App makes API request      │                                │
       │     with token in header       │                                │
       ├────────────────────────────────┼───────────────────────────────>│
       │     Authorization: Bearer eyJhbGc...                            │
       │                                │                                │
       │                                │  7. Backend fetches Clerk's    │
       │                                │     public keys (JWKS)         │
       │                                │<───────────────────────────────┤
       │                                │                                │
       │                                │  8. Public keys returned       │
       │                                ├───────────────────────────────>│
       │                                │                                │
       │                                │  9. Backend validates JWT      │
       │                                │     signature with public key  │
       │                                │                                │
       │  10. API response sent back    │                                │
       │<────────────────────────────────────────────────────────────────┤
       │                                │                                │
```

### Step-by-Step Breakdown

**Steps 1-5: User Login (Frontend + Clerk)**
- React Native app uses Clerk's SDK
- User authenticates with Clerk
- Clerk returns a JWT token
- App stores token (in memory or secure storage)

**Steps 6-10: API Request (Backend Validation)**
- App sends request with `Authorization: Bearer <token>` header
- Spring Security intercepts the request
- `ClerkJwtDecoder` fetches Clerk's public keys (JWKS)
- Validates token signature, expiration, issuer
- Extracts user info (clerkId) and creates `Authentication` object
- Request proceeds to controller

---

## Your Application's Security Flow

### File Structure

```
src/main/java/com/audibleclone/backend/
├── config/
│   └── SecurityConfig.java                   ← Main security configuration
├── security/
│   ├── ClerkJwtDecoder.java                  ← Validates JWT tokens
│   ├── ClerkJwtAuthenticationConverter.java  ← Converts JWT to Authentication
│   └── SecurityUtils.java                    ← Helper methods
```

### The Security Chain

```
1. HTTP Request arrives
        ↓
2. CORS Filter (SecurityConfig line 35)
        ↓
3. Security Filter Chain (SecurityConfig line 33)
        ↓
4. Check if endpoint is public (lines 52-56)
   ├─ /actuator/health → Allow ✅
   ├─ /swagger-ui/** → Allow ✅
   ├─ /api/v1/users/sync → Allow ✅
   └─ /api/v1/** → Requires authentication ⚠️
        ↓
5. Extract JWT from Authorization header
        ↓
6. ClerkJwtDecoder validates token (line 64)
   ├─ Fetch JWKS from Clerk
   ├─ Verify signature
   ├─ Check expiration
   └─ Validate issuer
        ↓
7. ClerkJwtAuthenticationConverter (line 65)
   ├─ Extract claims (sub, email)
   └─ Create Authentication object with roles
        ↓
8. Authorization check (@PreAuthorize, etc.)
        ↓
9. Request reaches controller
```

---

## Understanding SecurityConfig

Let's break down `SecurityConfig.java` section by section:

### 1. Class Annotations

**SecurityConfig.java (lines 23-27):**
```java
@Configuration                // Tells Spring this is a configuration class
@EnableWebSecurity            // Enables Spring Security
@EnableMethodSecurity         // Enables @PreAuthorize, @Secured annotations
@RequiredArgsConstructor      // Lombok: creates constructor with final fields
public class SecurityConfig {
    private final ClerkJwtDecoder clerkJwtDecoder;
    private final ClerkJwtAuthenticationConverter clerkJwtAuthenticationConverter;
}
```

**What This Means:**
- Spring will configure security based on this class
- Controllers can use `@PreAuthorize("isAuthenticated()")` for protection
- `ClerkJwtDecoder` and `ClerkJwtAuthenticationConverter` are injected via constructor

---

### 2. Security Filter Chain

**SecurityConfig.java (lines 32-82):**
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(headers -> /* security headers */)
        .authorizeHttpRequests(authorize -> /* authorization rules */)
        .oauth2ResourceServer(oauth2 -> /* JWT configuration */)
        .exceptionHandling(exceptions -> /* error handlers */);

    return http.build();
}
```

Let's break down each section:

---

#### A. CORS Configuration (line 35)

```java
.cors(cors -> cors.configurationSource(corsConfigurationSource()))
```

**What it does:** Enables Cross-Origin Resource Sharing
**Why:** Allows React Native app (different origin) to call your API

**Configured in lines 88-105:**
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    configuration.setAllowedOriginPatterns(List.of(
        "http://localhost:*",      // Local development
        "http://127.0.0.1:*",      // Local development (alternative)
        "exp://*"                  // Expo Go app
    ));

    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

**Translation:**
- Allow requests from `localhost` on any port (React Native dev server)
- Allow requests from Expo Go app (`exp://`)
- Allow all HTTP methods (GET, POST, PUT, DELETE, OPTIONS)
- Allow all headers (including `Authorization`)
- Allow credentials (cookies, auth headers)

---

#### B. CSRF Disabled (line 36)

```java
.csrf(csrf -> csrf.disable())
```

**What is CSRF?** Cross-Site Request Forgery attack
**Why disabled?**
- CSRF protection is for cookie-based authentication
- JWT tokens are in `Authorization` header (not cookies)
- React Native apps don't use cookies
- **If you used cookies, you'd enable CSRF!**

---

#### C. Stateless Session Management (line 37)

```java
.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

**What it does:** Tells Spring Security not to create HTTP sessions
**Why:**
- JWT is stateless (all info in token)
- No need for server-side session storage
- Scales better (no session sharing between servers)

---

#### D. Security Headers (lines 38-49)

```java
.headers(headers -> headers
    .frameOptions(frame -> frame.deny())              // Prevent clickjacking
    .contentTypeOptions(content -> {})                // Prevent MIME sniffing
    .httpStrictTransportSecurity(hstsConfig -> hstsConfig
        .maxAgeInSeconds(31536000)                    // HTTPS for 1 year
        .includeSubDomains(true)
    )
    .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.SAME_ORIGIN))
    .permissionsPolicy(permissions -> permissions
        .policy("camera=(), microphone=(), geolocation=()")  // Disable device APIs
    )
)
```

**Security Headers Explained:**

| Header | What it Does | Protection |
|--------|-------------|------------|
| `X-Frame-Options: DENY` | Prevents page from being embedded in iframe | Clickjacking attacks |
| `X-Content-Type-Options: nosniff` | Forces browser to respect Content-Type | MIME type attacks |
| `Strict-Transport-Security` | Forces HTTPS for 1 year | Man-in-the-middle attacks |
| `Referrer-Policy: same-origin` | Only send referrer to same origin | Information leakage |
| `Permissions-Policy` | Disables camera/mic/location | Privacy protection |

---

#### E. Authorization Rules (lines 50-61)

```java
.authorizeHttpRequests(authorize -> authorize
    // Public endpoints (no authentication needed)
    .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
    .requestMatchers("/api/v1/users/sync").permitAll()

    // Protected endpoints (authentication required)
    .requestMatchers("/api/v1/**").authenticated()

    // Deny everything else
    .anyRequest().denyAll()
)
```

**How It Works:**

```
Request: GET /actuator/health
    ↓
Matches: .requestMatchers("/actuator/health").permitAll()
    ↓
Result: ✅ Allowed (no authentication needed)

Request: GET /api/v1/lectures
    ↓
Matches: .requestMatchers("/api/v1/**").authenticated()
    ↓
Result: ⚠️ Requires valid JWT token

Request: GET /random-endpoint
    ↓
Matches: .anyRequest().denyAll()
    ↓
Result: ❌ 403 Forbidden
```

**Why `/api/v1/users/sync` is public:**
- Called during authentication flow (user doesn't have token yet!)
- Creates/updates user record in database after Clerk login
- Frontend calls this immediately after getting token from Clerk

---

#### F. JWT Configuration (lines 62-67)

```java
.oauth2ResourceServer(oauth2 -> oauth2
    .jwt(jwt -> jwt
        .decoder(clerkJwtDecoder)                           // How to decode JWT
        .jwtAuthenticationConverter(clerkJwtAuthenticationConverter)  // How to convert to Authentication
    )
)
```

**What This Does:**
1. Tells Spring Security: "We're using JWT tokens"
2. Use `ClerkJwtDecoder` to validate tokens
3. Use `ClerkJwtAuthenticationConverter` to extract user info

---

#### G. Exception Handling (lines 68-79)

```java
.exceptionHandling(exceptions -> exceptions
    .authenticationEntryPoint((request, response, authException) -> {
        response.setStatus(401);  // Unauthorized
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Authentication required\",\"message\":\"Valid JWT token is required to access this resource\"}");
    })
    .accessDeniedHandler((request, response, accessDeniedException) -> {
        response.setStatus(403);  // Forbidden
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Access denied\",\"message\":\"Insufficient privileges to access this resource\"}");
    })
)
```

**Two Types of Errors:**

1. **401 Unauthorized** (`authenticationEntryPoint`)
   - No JWT token provided
   - Invalid JWT token
   - Expired JWT token
   - **Message:** "You need to log in"

2. **403 Forbidden** (`accessDeniedHandler`)
   - Valid JWT token provided
   - User is authenticated
   - But lacks required permissions
   - **Example:** Regular user trying to access admin endpoint
   - **Message:** "You're logged in, but you can't do that"

---

## JWT Validation with ClerkJwtDecoder

**ClerkJwtDecoder.java** is responsible for validating JWT tokens from Clerk.

### How It Works

**ClerkJwtDecoder.java (lines 23-25):**
```java
public ClerkJwtDecoder(@Value("${audibleclone.clerk.jwt-issuer}") String clerkIssuer) {
    this.jwtDecoder = createJwtDecoder(clerkIssuer);
}
```

**What Happens:**
1. Reads `audibleclone.clerk.jwt-issuer` from configuration
2. Creates a `NimbusJwtDecoder` configured for Clerk
3. This decoder is used for all JWT validation

---

### Creating the Decoder

**ClerkJwtDecoder.java (lines 34-64):**
```java
private NimbusJwtDecoder createJwtDecoder(String issuer) {
    try {
        logger.info("Creating JWT decoder for issuer: {}", issuer);

        // Create validator that checks issuer, expiration, not-before
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withIssuer);

        // Try auto-discovery (fetch /.well-known/openid-configuration)
        NimbusJwtDecoder decoder;
        try {
            logger.info("Attempting auto-discovery for issuer: {}", issuer);
            decoder = JwtDecoders.fromIssuerLocation(issuer);
        } catch (Exception e) {
            logger.warn("Auto-discovery failed, trying manual JWKS URI");
            // Fallback: manually construct JWKS URL
            String jwksUri = issuer + "/.well-known/jwks.json";
            logger.info("Using manual JWKS URI: {}", jwksUri);
            decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        }

        decoder.setJwtValidator(validator);
        return decoder;
    } catch (Exception e) {
        logger.error("Failed to create JWT decoder", e);
        throw new IllegalStateException("Unable to configure JWT decoder for Clerk", e);
    }
}
```

**What It Does:**

1. **Creates Validator:**
   - Checks `iss` (issuer) claim matches Clerk
   - Checks `exp` (expiration) is in the future
   - Checks `nbf` (not before) is in the past

2. **Fetches JWKS (JSON Web Key Set):**
   - **Option A (auto-discovery):** Fetch from `https://strong-bison-1.clerk.accounts.dev/.well-known/openid-configuration`
   - **Option B (fallback):** Fetch from `https://strong-bison-1.clerk.accounts.dev/.well-known/jwks.json`

3. **Caches Public Keys:**
   - Spring Security caches JWKS
   - Refreshes automatically when keys rotate
   - No need to fetch on every request!

---

### Decoding a Token

**ClerkJwtDecoder.java (lines 73-83):**
```java
@Override
public Jwt decode(String token) throws JwtException {
    try {
        return jwtDecoder.decode(token);
    } catch (JwtException e) {
        logger.debug("JWT validation failed: {}", e.getMessage());
        throw e;  // Let Spring Security handle the error
    }
}
```

**What Happens When Token is Decoded:**

1. **Extract header, payload, signature from token**
2. **Fetch public key from JWKS** (using `kid` from header)
3. **Verify signature:** `verify(payload + signature, publicKey)`
4. **Check claims:**
   - `iss` matches expected issuer
   - `exp` (expiration) is in future
   - `nbf` (not before) is in past
5. **If valid:** Return `Jwt` object with claims
6. **If invalid:** Throw `JwtException` → Returns 401 Unauthorized

---

## JWKS and Public Key Cryptography

### What is JWKS?

**JWKS** = **JSON Web Key Set** (a list of public keys)

**Example JWKS from Clerk:**
```json
{
  "keys": [
    {
      "kty": "RSA",
      "use": "sig",
      "kid": "ins_2a1b3c4d",
      "n": "0vx7agoebGcQSuuPiLJXZptN9...",  // Public key (modulus)
      "e": "AQAB"                            // Public key (exponent)
    },
    {
      "kty": "RSA",
      "use": "sig",
      "kid": "ins_5e6f7g8h",
      "n": "xjlbcH3qR2F9vK8Lm4n...",
      "e": "AQAB"
    }
  ]
}
```

### How Public Key Cryptography Works

**Analogy: Mailbox**
- **Private Key** = Key to open the mailbox (only Clerk has this)
- **Public Key** = Mailbox slot (anyone can verify, but only Clerk can create)

**JWT Signing Flow:**
```
Clerk (has private key):
1. Creates JWT payload: { "sub": "user_123", "exp": 1700000000 }
2. Signs with private key: signature = sign(payload, privateKey)
3. Sends JWT to client: header.payload.signature

Your Backend (has public key):
1. Receives JWT from client
2. Extracts header, payload, signature
3. Verifies: verify(signature, payload, publicKey)
4. If valid: Trust the payload data
```

**Key Point:**
- Only Clerk can **create** valid signatures (has private key)
- Anyone can **verify** signatures (public key is... public!)
- If signature is valid, payload hasn't been tampered with

### Why JWKS Instead of Hardcoded Public Key?

**Benefits:**
1. **Key Rotation:** Clerk can rotate keys without breaking your app
2. **Multiple Keys:** Clerk can use different keys for different purposes
3. **Automatic Updates:** Spring Security fetches new keys automatically

---

## Authorization with Roles

### ClerkJwtAuthenticationConverter

**ClerkJwtAuthenticationConverter.java (lines 16-26):**
```java
@Override
public AbstractAuthenticationToken convert(Jwt jwt) {
    Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
    return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
}

private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
    List<GrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));  // Every authenticated user
    return authorities;
}
```

**What This Does:**
1. Extracts user ID from JWT (`jwt.getSubject()`)
2. Assigns role `ROLE_USER` to every authenticated user
3. Creates `JwtAuthenticationToken` with user info and roles

### Using Roles in Controllers

**Example:**
```java
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    // Only authenticated users can access
    @GetMapping("/users")
    @PreAuthorize("isAuthenticated()")
    public List<UserDto> getAllUsers() {
        // ...
    }

    // Only users with ROLE_ADMIN can access
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(@PathVariable Long id) {
        // ...
    }
}
```

**How to Add Admin Role:**

Currently, **every authenticated user gets `ROLE_USER`**. To add admin roles:

**Option 1: Check email (simple)**
```java
private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
    List<GrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

    // Check if email is in admin list
    String email = jwt.getClaimAsString("email");
    if (email != null && email.equals("dalanhabib@gmail.com")) {
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    return authorities;
}
```

**Option 2: Use Clerk metadata (recommended)**
```java
private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
    List<GrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

    // Clerk can include custom claims in JWT
    Map<String, Object> metadata = jwt.getClaimAsMap("public_metadata");
    if (metadata != null && Boolean.TRUE.equals(metadata.get("isAdmin"))) {
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    return authorities;
}
```

---

## CORS Configuration

### What is CORS?

**CORS** = **Cross-Origin Resource Sharing**

### The Problem

**Without CORS:**
```
React Native App (http://localhost:8081)
    ↓ Makes request to
Backend API (http://localhost:8080)
    ↓
Browser/System: ❌ BLOCKED! Different origin!
```

**With CORS:**
```
React Native App (http://localhost:8081)
    ↓ Makes request to
Backend API (http://localhost:8080)
    ↓ Checks CORS config
    ↓ "http://localhost:*" is allowed
    ↓
✅ Request proceeds
```

### Your CORS Configuration

**SecurityConfig.java (lines 92-96):**
```java
configuration.setAllowedOriginPatterns(List.of(
    "http://localhost:*",      // Local dev server (any port)
    "http://127.0.0.1:*",      // Alternative localhost
    "exp://*"                  // Expo Go app
));
```

**Why Wildcards?**
- React Native dev server uses random ports (19000, 19006, etc.)
- Expo Go uses `exp://` protocol
- Pattern matching allows any port

**Production CORS:**

In `application-prod.yml`:
```yaml
audibleclone:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:https://your-react-native-app.com}
```

Set environment variable:
```bash
CORS_ALLOWED_ORIGINS=https://app.example.com,https://mobile.example.com
```

---

## Security Headers

Your application adds several security headers to every response:

### 1. X-Frame-Options: DENY

**Prevents:** Clickjacking attacks
**How:** Browser won't allow page to be embedded in `<iframe>`

**Attack prevented:**
```html
<!-- Malicious site -->
<iframe src="https://your-api.com/admin/delete-all-data"></iframe>
<!-- User clicks "Win iPhone!" button, actually deletes data -->
```

---

### 2. X-Content-Type-Options: nosniff

**Prevents:** MIME type sniffing attacks
**How:** Browser must respect Content-Type header

**Attack prevented:**
```
Hacker uploads file "image.jpg" that's actually JavaScript
Without header: Browser might execute it
With header: Browser treats it as image (safe)
```

---

### 3. Strict-Transport-Security (HSTS)

**Prevents:** Man-in-the-middle attacks
**How:** Forces HTTPS for 1 year, including subdomains

```java
.httpStrictTransportSecurity(hstsConfig -> hstsConfig
    .maxAgeInSeconds(31536000)   // 1 year
    .includeSubDomains(true)
)
```

**What it does:**
```
First visit: https://api.example.com
    ↓ Server sends: Strict-Transport-Security: max-age=31536000
    ↓ Browser remembers: "Always use HTTPS for this domain"

Later, user types: http://api.example.com
    ↓ Browser automatically: https://api.example.com
    ↓ No insecure HTTP request ever sent!
```

---

### 4. Referrer-Policy: same-origin

**Prevents:** Information leakage
**How:** Only sends Referer header to same origin

```
User on: https://api.example.com/user/profile?id=123
Clicks link to: https://external-site.com
    ↓
Without policy: Referer: https://api.example.com/user/profile?id=123  ❌ Leaks user ID!
With same-origin: (no Referer header sent)  ✅ Privacy protected
```

---

### 5. Permissions-Policy

**Prevents:** Unauthorized device access
**How:** Disables camera, microphone, geolocation APIs

```java
.permissionsPolicy(permissions -> permissions
    .policy("camera=(), microphone=(), geolocation=()")
)
```

**What it does:**
```javascript
// In browser/webview, this would fail:
navigator.geolocation.getCurrentPosition()  // ❌ Blocked by policy

// Even if attacker injects JavaScript, they can't access device features
```

---

## Common Security Patterns

### 1. Accessing Authenticated User

**In a Controller:**
```java
@GetMapping("/me")
public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
    String clerkId = authentication.getName();  // Clerk user ID
    // ... lookup user
}
```

**Using SecurityUtils:**
```java
@GetMapping("/me")
public ResponseEntity<UserDto> getCurrentUser() {
    String clerkId = SecurityUtils.getCurrentUserId();
    // ... lookup user
}
```

### 2. Protecting Endpoints

**Method-level:**
```java
@GetMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public List<UserDto> getAllUsers() {
    // Only admins can access
}
```

**Check authentication programmatically:**
```java
@GetMapping("/profile/{id}")
public UserDto getProfile(@PathVariable Long id, Authentication auth) {
    String currentUserId = auth.getName();

    // Users can only view their own profile
    if (!currentUserId.equals(userId)) {
        throw new AccessDeniedException("Cannot view other user's profile");
    }

    // ... return profile
}
```

### 3. Optional Authentication

**Some endpoints work better with auth, but don't require it:**
```java
@GetMapping("/lectures")
public Page<LectureDto> getLectures(
    Pageable pageable,
    @AuthenticationPrincipal Jwt jwt  // Can be null!
) {
    if (jwt != null) {
        // User is logged in - return personalized results
        String userId = jwt.getSubject();
        return lectureService.getPersonalizedLectures(userId, pageable);
    } else {
        // User not logged in - return public lectures
        return lectureService.getPublicLectures(pageable);
    }
}
```

---

## Troubleshooting

### 1. "401 Unauthorized" Error

**Symptoms:**
```json
{
  "error": "Authentication required",
  "message": "Valid JWT token is required to access this resource"
}
```

**Possible Causes:**

**A. No token in request**
```bash
# ❌ Missing Authorization header
curl http://localhost:8081/api/v1/lectures

# ✅ Include token
curl -H "Authorization: Bearer eyJhbGc..." http://localhost:8081/api/v1/lectures
```

**B. Expired token**
```java
// Check token expiration
{
  "exp": 1700000000  // January 1, 2024 (expired!)
}
```
**Solution:** Get a new token from Clerk

**C. Wrong issuer**
```yaml
# application.yml
audibleclone:
  clerk:
    jwt-issuer: https://strong-bison-1.clerk.accounts.dev  # Must match Clerk instance!
```

**D. JWKS fetch failed**
```
ERROR: Failed to fetch JWKS from https://.../.well-known/jwks.json
```
**Solution:** Check network connectivity, Clerk configuration

---

### 2. "403 Forbidden" Error

**Symptoms:**
```json
{
  "error": "Access denied",
  "message": "Insufficient privileges to access this resource"
}
```

**Cause:** User is authenticated but lacks required role

```java
@PreAuthorize("hasRole('ADMIN')")  // User has ROLE_USER, not ROLE_ADMIN
public void deleteUser() { }
```

**Solution:** Check role assignment in `ClerkJwtAuthenticationConverter`

---

### 3. CORS Errors

**Symptoms:**
```
Access to fetch at 'http://localhost:8081/api/v1/lectures' from origin
'http://localhost:19006' has been blocked by CORS policy
```

**Solution:** Add origin to allowed patterns
```java
configuration.setAllowedOriginPatterns(List.of(
    "http://localhost:*",
    "http://127.0.0.1:*",
    "exp://*",
    "http://192.168.1.*"  // Add your local network IP
));
```

---

### 4. Token in Wrong Format

**❌ Common mistakes:**
```
Authorization: eyJhbGc...                    // Missing "Bearer "
Authorization: Bearer: eyJhbGc...            // Extra colon
Authorization: bearer eyJhbGc...             // Lowercase
```

**✅ Correct format:**
```
Authorization: Bearer eyJhbGc...
```

---

## Summary

### Security Flow Recap

```
1. User logs in with Clerk (frontend)
    ↓
2. Clerk returns JWT token
    ↓
3. Frontend includes token in requests: Authorization: Bearer <token>
    ↓
4. Spring Security intercepts request
    ↓
5. ClerkJwtDecoder validates token:
   - Fetch JWKS (public keys) from Clerk
   - Verify signature
   - Check expiration
   - Validate issuer
    ↓
6. ClerkJwtAuthenticationConverter extracts user info and roles
    ↓
7. Spring Security creates Authentication object
    ↓
8. Request proceeds to controller with authenticated user
```

### Key Files

| File | Purpose |
|------|---------|
| `SecurityConfig.java` | Main security configuration (CORS, authorization rules, JWT setup) |
| `ClerkJwtDecoder.java` | Validates JWT tokens from Clerk |
| `ClerkJwtAuthenticationConverter.java` | Converts JWT to Spring Security Authentication |
| `SecurityUtils.java` | Helper methods for accessing current user |

### Important Concepts

1. **JWT = Stateless authentication** (no sessions needed)
2. **Clerk = Third-party auth provider** (handles login, password reset, etc.)
3. **JWKS = Public keys** (used to verify JWT signatures)
4. **Roles = Authorization** (ROLE_USER, ROLE_ADMIN, etc.)
5. **CORS = Cross-origin requests** (allows React Native app to call API)

### Quick Reference

**Get current user:**
```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String clerkId = auth.getName();
```

**Protect endpoint:**
```java
@PreAuthorize("isAuthenticated()")
public void protectedMethod() { }
```

**Check role:**
```java
@PreAuthorize("hasRole('ADMIN')")
public void adminOnly() { }
```

---

**Next Steps:**
- Read the **DTOs and DTO Pattern** guide to understand how authenticated users' data is safely exposed via APIs
- Experiment with adding admin roles
- Try accessing protected endpoints with/without tokens

---

**Related Topics:**
- [Understanding Spring Boot Configuration & Profiles](./understanding-spring-boot-configuration.md)
- [Understanding DTOs and the DTO Pattern](./understanding-dtos-pattern.md)
- [Understanding Dependency Injection & Spring Beans](./understanding-dependency-injection.md)
