# Understanding Spring Boot Configuration & Profiles

## Table of Contents
1. [What is Configuration?](#what-is-configuration)
2. [Why Use Configuration Files?](#why-use-configuration-files)
3. [YAML vs Properties Files](#yaml-vs-properties-files)
4. [Your Application's Configuration Files](#your-applications-configuration-files)
5. [Understanding Spring Profiles](#understanding-spring-profiles)
6. [Configuration Deep Dive](#configuration-deep-dive)
7. [Environment Variables](#environment-variables)
8. [How Configuration is Loaded](#how-configuration-is-loaded)
9. [Common Patterns](#common-patterns)
10. [Best Practices](#best-practices)

---

## What is Configuration?

**Configuration** is how you tell your application:
- Which database to connect to
- What credentials to use
- How to behave in different environments (development vs production)
- Feature toggles (enable/disable features)
- Performance tuning (connection pool sizes, timeouts, etc.)

### Analogy
Think of configuration like the settings on your phone:
- Some settings are the same for everyone (default ringtone volume level)
- Some settings change based on where you are (WiFi networks available)
- Some settings are secrets (your WiFi password)

Configuration does the same thing for your application!

---

## Why Use Configuration Files?

### The Problem Without Configuration

Imagine if database credentials were hardcoded:

```java
// BAD: Hardcoded values ❌
public class DatabaseConnector {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/mydb";
    private static final String DB_USER = "admin";
    private static final String DB_PASSWORD = "secret123";  // 😱 Secret in code!

    // Now every time you deploy to production, you need to change the code!
}
```

**Problems:**
1. ❌ Can't use different databases for dev/test/prod without changing code
2. ❌ Secrets are committed to version control (security risk!)
3. ❌ Can't change settings without recompiling
4. ❌ Different developers have different local setups

### The Solution: Configuration Files

```yaml
# application.yml ✅
spring:
  datasource:
    url: ${DATABASE_URL}           # From environment variable
    username: ${DB_USERNAME}       # From environment variable
    password: ${DB_PASSWORD}       # From environment variable (secret!)
```

**Benefits:**
1. ✅ Same code works in all environments
2. ✅ Secrets stay out of version control
3. ✅ Change settings without recompiling
4. ✅ Each developer/environment has their own settings

---

## YAML vs Properties Files

Spring Boot supports two configuration formats:

### Properties Format (`.properties`)
```properties
# application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
spring.datasource.username=admin
spring.jpa.show-sql=true
logging.level.com.elmify=DEBUG
```

### YAML Format (`.yml`) ⭐ What You Use
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: admin
  jpa:
    show-sql: true
logging:
  level:
    com.elmify: DEBUG
```

**Why YAML is Better:**
- ✅ More readable (hierarchical structure)
- ✅ Less repetition (`spring.datasource` vs `spring:` → `datasource:`)
- ✅ Easier to organize complex configurations
- ✅ Supports lists and maps naturally

**Your Application Uses YAML** for these reasons!

---

## Your Application's Configuration Files

Your application has **three** configuration files:

```
src/main/resources/
├── application.yml          # Base configuration (shared across all environments)
├── application-dev.yml      # Development-specific settings
└── application-prod.yml     # Production-specific settings
```

### How They Work Together

```
application.yml
    ↓ (base settings)
    ↓
application-dev.yml  OR  application-prod.yml
    ↓ (environment-specific overrides)
    ↓
Final Configuration
```

**Merge Strategy:**
1. Spring loads `application.yml` first (base settings)
2. Then loads `application-{profile}.yml` (e.g., `application-dev.yml`)
3. Profile-specific settings **override** base settings
4. Settings not in profile file **keep** base values

---

## Understanding Spring Profiles

### What is a Profile?

A **profile** is a **label** for an environment. Your application has two profiles:
- `dev` (development - your laptop)
- `prod` (production - deployed server)

### Activating a Profile

**In `application.yml` (line 9):**
```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
```

**Translation:**
- Read environment variable `SPRING_PROFILES_ACTIVE`
- If it doesn't exist, default to `dev`
- This activates `application-dev.yml`

**Setting the Profile:**

**Option 1: Environment Variable** (Production)
```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar elmify-backend.jar
```

**Option 2: Command Line** (Testing)
```bash
java -jar elmify-backend.jar --spring.profiles.active=prod
```

**Option 3: IDE Configuration** (Development)
- IntelliJ: Run Configuration → Environment Variables → `SPRING_PROFILES_ACTIVE=dev`
- Default is `dev` if not set

### Profile Visualization

```
Local Development:
SPRING_PROFILES_ACTIVE=dev
    ↓
application.yml + application-dev.yml
    ↓
- Database: localhost:5432
- Logging: DEBUG level
- Storage: MinIO (local S3)
- Show SQL: true
- CORS: Allow all origins

Production Server:
SPRING_PROFILES_ACTIVE=prod
    ↓
application.yml + application-prod.yml
    ↓
- Database: ${DATABASE_URL} (from environment)
- Logging: WARN level
- Storage: Cloudflare R2 (real S3)
- Show SQL: false
- CORS: Specific origins only
```

---

## Configuration Deep Dive

Let's explore key sections of your configuration:

### 1. Application Metadata

**`application.yml` (lines 4-9):**
```yaml
spring:
  application:
    name: elmify-backend  # Application name (used in logs, monitoring)

  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}  # Which profile to activate
```

**Why Important:**
- `name` appears in logs and monitoring tools
- `profiles.active` determines dev vs prod behavior

---

### 2. Database Configuration

**`application-dev.yml` (lines 9-19):**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/elmify_db  # Local PostgreSQL
    username: alanhabib
    password: password
    hikari:
      connection-timeout: 20000      # 20 seconds
      maximum-pool-size: 10          # Max 10 connections
      minimum-idle: 5                # Keep 5 connections ready
      idle-timeout: 300000           # Close idle connections after 5 min
      max-lifetime: 1200000          # Recycle connections after 20 min
      auto-commit: true
```

**`application-prod.yml` (lines 4-14):**
```yaml
spring:
  datasource:
    url: ${DATABASE_URL}              # From environment variable!
    username: ${DB_USERNAME}          # From environment variable!
    password: ${DB_PASSWORD}          # From environment variable!
    hikari:
      connection-timeout: 30000       # 30 seconds (longer for network delays)
      maximum-pool-size: 20           # More connections for production load
      minimum-idle: 5
      idle-timeout: 600000            # 10 minutes
      max-lifetime: 1800000           # 30 minutes
      leak-detection-threshold: 60000 # Warn if connection held >1 min
```

**Key Differences:**

| Setting | Dev | Prod | Why Different? |
|---------|-----|------|----------------|
| URL | `localhost:5432` | `${DATABASE_URL}` | Dev uses local DB, prod uses cloud DB |
| Credentials | Hardcoded | Environment variables | Secrets shouldn't be in files! |
| Connection Pool Size | 10 | 20 | Production handles more traffic |
| Timeouts | Shorter | Longer | Production has network latency |

**What is HikariCP?**
HikariCP is the **connection pool** - it maintains a pool of database connections so your app doesn't create/destroy connections constantly (which is slow).

---

### 3. JPA/Hibernate Configuration

**`application.yml` (lines 20-33):**
```yaml
spring:
  jpa:
    open-in-view: false                  # Don't keep transactions open during view rendering
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect  # PostgreSQL-specific SQL
        format_sql: false                # Don't format SQL in logs (base setting)
        jdbc:
          batch_size: 25                 # Insert/update 25 records at a time
          order_inserts: true            # Order inserts for better batching
          order_updates: true            # Order updates for better batching
        connection:
          provider_disables_autocommit: false
    hibernate:
      ddl-auto: validate                 # Verify schema matches entities (don't auto-create)
```

**`application-dev.yml` (lines 21-31):**
```yaml
spring:
  jpa:
    show-sql: true                       # Print SQL queries in dev
    properties:
      hibernate:
        format_sql: true                 # Format SQL for readability
        use_sql_comments: true           # Add comments to SQL queries
```

**`application-prod.yml` (lines 16-28):**
```yaml
spring:
  jpa:
    show-sql: false                      # Don't print SQL (performance + security)
    properties:
      hibernate:
        format_sql: false                # No formatting needed
        use_sql_comments: false
        jdbc:
          batch_size: 50                 # Larger batches in production
        cache:
          use_second_level_cache: true   # Enable caching for performance
          use_query_cache: true
```

**Key Takeaway:**
- **Dev:** Verbose logging, smaller batches, no caching (easier debugging)
- **Prod:** Minimal logging, larger batches, caching enabled (better performance)

---

### 4. Flyway (Database Migrations)

**`application.yml` (lines 35-39):**
```yaml
spring:
  flyway:
    enabled: true                        # Enable Flyway migrations
    locations: classpath:db/migration    # Where migration SQL files are
    baseline-on-migrate: true            # Allow migrating existing database
    validate-on-migrate: true            # Validate migrations before applying
```

**`application-dev.yml` (line 34):**
```yaml
spring:
  flyway:
    clean-disabled: false                # Allow dropping all tables (dangerous!)
```

**`application-prod.yml` (lines 30-32):**
```yaml
spring:
  flyway:
    clean-disabled: true                 # NEVER allow dropping tables in production!
    validate-on-migrate: true
```

**Why Different:**
- In dev, you might want to reset your database (`flyway:clean`)
- In production, accidentally dropping all tables would be catastrophic!

---

### 5. Security Configuration

**`application.yml` (lines 41-46):**
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: https://api.clerk.com/v1/jwks  # Where to get Clerk's public keys
```

**Custom Clerk Configuration:**

**`application.yml` (lines 104-107):**
```yaml
audibleclone:
  clerk:
    secret-key: ${CLERK_SECRET_KEY}                  # From environment variable
    publishable-key: ${CLERK_PUBLISHABLE_KEY}        # From environment variable
    jwt-issuer: ${CLERK_JWT_ISSUER:https://strong-bison-1.clerk.accounts.dev}
```

**What These Do:**
- `jwk-set-uri`: Spring Security fetches Clerk's public keys to validate JWTs
- `secret-key`: Used by backend to make authenticated requests to Clerk API
- `publishable-key`: Sent to frontend (safe to expose)
- `jwt-issuer`: Validates JWT tokens came from your Clerk instance

---

### 6. Storage Configuration (MinIO vs R2)

This is one of the **coolest** examples of profile-based configuration!

**`application-dev.yml` (lines 57-64):**
```yaml
audibleclone:
  r2:
    # For local development, use MinIO (local S3-compatible server)
    endpoint: http://127.0.0.1:9000
    bucket-name: elmify-audio
    access-key: minioadmin               # Default MinIO credentials
    secret-key: minioadmin
    region: us-east-1
    presigned-url-expiration: PT1H       # 1 hour
```

**`application-prod.yml` (lines 66-73):**
```yaml
audibleclone:
  r2:
    # Production R2 configuration (Cloudflare S3)
    endpoint: https://b995be98e08909685abfca00c971e79e.r2.cloudflarestorage.com
    bucket-name: elmify-audio
    access-key: ${R2_ACCESS_KEY}         # From environment variable (secret!)
    secret-key: ${R2_SECRET_KEY}         # From environment variable (secret!)
    region: auto
    presigned-url-expiration: PT30M      # 30 minutes (more secure)
```

**The Magic:**
Your `StorageService.java` uses these settings without knowing whether it's MinIO or R2!

```java
@Service
public class StorageService {
    @Value("${elmify.r2.endpoint}")
    private String endpoint;  // Automatically "http://127.0.0.1:9000" in dev
                              //              or R2 URL in prod!
}
```

**Result:**
- Local development: Audio files stored in MinIO on your laptop
- Production: Audio files stored in Cloudflare R2
- **Same code** works in both environments!

---

### 7. Logging Configuration

**`application.yml` (lines 74-83):**
```yaml
logging:
  level:
    root: INFO                           # Default log level for all libraries
    com.elmify: INFO               # Your application's log level
    org.springframework.security: INFO
    org.springframework.web: INFO
    org.hibernate.SQL: INFO              # SQL query logging
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

**`application-dev.yml` (lines 41-50):**
```yaml
logging:
  level:
    com.elmify: DEBUG              # See DEBUG messages
    org.springframework.security: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG             # See all SQL queries
    org.hibernate.type: TRACE            # See parameter values in queries!
    org.springframework.transaction: DEBUG
  file:
    name: logs/elmify-dev.log      # Write to file
```

**`application-prod.yml` (lines 46-56):**
```yaml
logging:
  level:
    root: WARN                           # Only warnings and errors
    com.elmify: INFO
    org.springframework.security: WARN
  pattern:
    console: "%d{ISO8601} [%thread] %-5level [%X{traceId:-},%X{spanId:-}] %logger{36} - %msg%n"
  file:
    name: /var/log/elmify/application.log
    max-size: 100MB                      # Rotate after 100MB
    max-history: 30                      # Keep 30 days of logs
```

**Log Levels (most to least verbose):**
1. `TRACE` - Everything (rarely used)
2. `DEBUG` - Detailed info for debugging
3. `INFO` - Important events
4. `WARN` - Warnings (something unexpected but not broken)
5. `ERROR` - Errors (something broke)

**Why Different:**
- **Dev:** Lots of logging helps debugging (DEBUG/TRACE)
- **Prod:** Less logging for performance (WARN/INFO)

---

### 8. Server Configuration

**`application.yml` (lines 49-56):**
```yaml
server:
  port: ${PORT:8081}                     # Use PORT env var, default to 8081
  error:
    include-message: always              # Include error messages in responses
    include-binding-errors: always       # Include validation errors
  compression:
    enabled: true                        # Compress HTTP responses
    mime-types: application/json,application/xml,text/html,text/xml,text/plain,application/javascript,text/css
```

**`application-prod.yml` (lines 35-43):**
```yaml
server:
  port: ${PORT:8080}                     # Default to 8080 in prod
  tomcat:
    max-threads: 200                     # Handle up to 200 concurrent requests
    min-spare-threads: 10                # Keep 10 threads ready
    connection-timeout: 20000            # Close idle connections after 20s
  compression:
    enabled: true
  forward-headers-strategy: framework    # Trust X-Forwarded-* headers (behind proxy)
```

**Why Different:**
- **Dev:** Simpler, minimal tuning
- **Prod:** Performance tuning for high traffic, proxy awareness

---

### 9. Management Endpoints (Actuator)

**`application.yml` (lines 59-71):**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus  # Which endpoints to expose
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized              # Hide details from public
  metrics:
    export:
      prometheus:
        enabled: true
```

**`application-dev.yml` (lines 81-85):**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"                             # Expose ALL endpoints in dev
```

**`application-prod.yml` (lines 80-93):**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus  # Only specific endpoints
  endpoint:
    health:
      show-details: never                        # Never show health details publicly
  metrics:
    export:
      prometheus:
        enabled: true
        step: PT1M                               # Export metrics every 1 minute
```

**Actuator Endpoints:**
- `/actuator/health` - Is the app running? Is DB reachable?
- `/actuator/info` - Application version and metadata
- `/actuator/metrics` - CPU, memory, request counts, etc.
- `/actuator/prometheus` - Metrics in Prometheus format

**Security:**
- **Dev:** Expose all endpoints for debugging
- **Prod:** Expose only necessary endpoints, hide sensitive details

---

## Environment Variables

### What are Environment Variables?

Environment variables are **settings stored outside your code**, set by the operating system or deployment platform.

### Syntax in YAML

```yaml
property: ${VARIABLE_NAME:default_value}
```

**Examples:**

```yaml
# Use DATABASE_URL from environment, or fail if not set
spring.datasource.url: ${DATABASE_URL}

# Use PORT from environment, or default to 8081
server.port: ${PORT:8081}

# Use CLERK_SECRET_KEY from environment, or use test key
audibleclone.clerk.secret-key: ${CLERK_SECRET_KEY:sk_test_dummy}
```

### Your Application's Environment Variables

| Variable | Used In | Purpose |
|----------|---------|---------|
| `SPRING_PROFILES_ACTIVE` | All | Selects dev or prod profile |
| `DATABASE_URL` | Prod | PostgreSQL connection string |
| `DB_USERNAME` | Prod | Database username |
| `DB_PASSWORD` | Prod | Database password |
| `R2_ACCESS_KEY` | Prod | Cloudflare R2 access key |
| `R2_SECRET_KEY` | Prod | Cloudflare R2 secret key |
| `CLERK_SECRET_KEY` | All | Clerk API secret |
| `CLERK_PUBLISHABLE_KEY` | All | Clerk publishable key |
| `CLERK_JWT_ISSUER` | All | Clerk JWT issuer URL |
| `PORT` | All | HTTP server port |

### Setting Environment Variables

**Local Development (macOS/Linux):**
```bash
export CLERK_SECRET_KEY=sk_test_abc123
export SPRING_PROFILES_ACTIVE=dev
./mvnw spring-boot:run
```

**IntelliJ IDEA:**
1. Run → Edit Configurations
2. Environment Variables → Add
3. `CLERK_SECRET_KEY=sk_test_abc123`

**Production (Docker/Cloud):**
```yaml
# docker-compose.yml
environment:
  - SPRING_PROFILES_ACTIVE=prod
  - DATABASE_URL=jdbc:postgresql://db:5432/mydb
  - DB_USERNAME=admin
  - DB_PASSWORD=secret
```

---

## How Configuration is Loaded

### Load Order (Priority)

Spring Boot loads configuration in this order (later sources override earlier):

1. **Default values in code** (`@Value("${prop:default}")`)
2. **`application.yml`** (base configuration)
3. **`application-{profile}.yml`** (profile-specific)
4. **Environment variables** (OS-level)
5. **Command-line arguments** (`--server.port=9090`)

**Example:**

```yaml
# application.yml
server.port: 8081

# application-prod.yml
server.port: 8080

# Environment variable
PORT=9000

# Command line
--server.port=7000
```

**Result:** Server runs on port `7000` (command line wins!)

### Visual Representation

```
┌─────────────────────────────┐
│ application.yml             │  server.port: 8081
│ (base settings)             │  logging.level: INFO
└─────────────────────────────┘
            ↓ (overrides)
┌─────────────────────────────┐
│ application-dev.yml         │  logging.level: DEBUG
│ (profile settings)          │  (server.port: still 8081)
└─────────────────────────────┘
            ↓ (overrides)
┌─────────────────────────────┐
│ Environment Variables       │  PORT=9000
│                             │  (logging.level: still DEBUG)
└─────────────────────────────┘
            ↓
Final Configuration:
  server.port: 9000       ← from env var
  logging.level: DEBUG    ← from profile
```

---

## Common Patterns

### 1. Sensitive Data Pattern

**❌ Bad:**
```yaml
# application.yml - NEVER DO THIS!
spring:
  datasource:
    password: mySecretPassword123  # In version control!
```

**✅ Good:**
```yaml
# application.yml
spring:
  datasource:
    password: ${DB_PASSWORD}  # From environment variable

# application-dev.yml (for convenience, but should be in .gitignore or .env)
spring:
  datasource:
    password: dev_password    # Only for local dev
```

### 2. Feature Toggle Pattern

```yaml
# application.yml
audibleclone:
  features:
    new-audio-player: ${ENABLE_NEW_PLAYER:false}

# application-dev.yml
audibleclone:
  features:
    new-audio-player: true  # Always enabled in dev for testing
```

```java
@Service
public class AudioService {
    @Value("${elmify.features.new-audio-player}")
    private boolean useNewPlayer;

    public void playAudio() {
        if (useNewPlayer) {
            // New implementation
        } else {
            // Old implementation
        }
    }
}
```

### 3. Duration Pattern

Spring Boot understands duration formats:

```yaml
audibleclone:
  r2:
    presigned-url-expiration: PT1H   # 1 hour
    #                         PT30M  # 30 minutes
    #                         PT90S  # 90 seconds
    #                         P1D    # 1 day
```

**Format:** `P` (period) `T` (time) `{number}{unit}`
- `S` = seconds
- `M` = minutes
- `H` = hours
- `D` = days

### 4. List Pattern

```yaml
# Multiple values
audibleclone:
  security:
    admin-emails: ${ADMIN_EMAILS:dalanhabib@gmail.com,admin@example.com}

# Or as a list
audibleclone:
  cors:
    allowed-origins:
      - http://localhost:3000
      - http://localhost:8085
      - https://app.example.com
```

---

## Best Practices

### 1. ✅ Use Profiles for Environments

**Why:** Keeps environment-specific settings separate and organized.

```
✅ application.yml          (shared settings)
✅ application-dev.yml      (dev overrides)
✅ application-prod.yml     (prod overrides)

❌ application-dev.yml      (everything duplicated)
❌ application-prod.yml     (everything duplicated)
```

### 2. ✅ Use Environment Variables for Secrets

**Why:** Keeps secrets out of version control.

```yaml
✅ password: ${DB_PASSWORD}
❌ password: mySecretPassword
```

### 3. ✅ Provide Defaults Where Appropriate

**Why:** Application still works if env var is missing.

```yaml
✅ server.port: ${PORT:8081}          # Defaults to 8081
❌ server.port: ${PORT}                # Fails if PORT not set
```

### 4. ✅ Document Your Configuration

Add comments explaining why settings exist:

```yaml
spring:
  jpa:
    open-in-view: false  # Prevent lazy loading exceptions in controllers
    hibernate:
      ddl-auto: validate # Never auto-create tables (use Flyway instead)
```

### 5. ✅ Use Meaningful Property Names

```yaml
✅ audibleclone.r2.presigned-url-expiration: PT1H
❌ app.url.exp: PT1H
```

### 6. ✅ Group Related Properties

```yaml
✅ audibleclone:
     r2:
       endpoint: ...
       bucket-name: ...
       access-key: ...

❌ audibleclone.r2.endpoint: ...
   audibleclone.r2.bucket.name: ...
   audibleclone.r2.key.access: ...
```

### 7. ✅ Different Settings for Different Environments

**Development:**
- Verbose logging (DEBUG)
- Show SQL queries
- Smaller connection pools
- Longer timeouts (for debugging)
- Allow CORS from all origins

**Production:**
- Minimal logging (WARN/INFO)
- Hide SQL queries
- Larger connection pools
- Shorter timeouts
- Restrict CORS to specific origins
- Enable caching
- More security restrictions

---

## Summary

### Configuration Files Overview

| File | Purpose | When Loaded |
|------|---------|-------------|
| `application.yml` | Base configuration (shared) | Always |
| `application-dev.yml` | Development overrides | When `SPRING_PROFILES_ACTIVE=dev` |
| `application-prod.yml` | Production overrides | When `SPRING_PROFILES_ACTIVE=prod` |

### Key Concepts

1. **Profiles** = Environment labels (dev, prod)
2. **Environment Variables** = Secrets and environment-specific values
3. **YAML Syntax** = Hierarchical, readable configuration format
4. **Override Order** = application.yml → profile → env vars → command line
5. **Externalized Config** = Keep secrets out of code

### The Magic of Profiles

**Same codebase, different behavior:**
```
dev profile:
├─ Local PostgreSQL
├─ MinIO storage
├─ DEBUG logging
└─ All CORS origins allowed

prod profile:
├─ Cloud PostgreSQL
├─ Cloudflare R2 storage
├─ WARN logging
└─ Specific CORS origins only
```

### Quick Reference

**Check active profile:**
```java
@Value("${spring.profiles.active}")
private String activeProfile;
```

**Read custom property:**
```java
@Value("${elmify.r2.bucket-name}")
private String bucketName;
```

**Conditional bean based on profile:**
```java
@Profile("dev")
@Component
public class DevOnlyService { }
```

---

**Next Steps:**
- Read the **Spring Security & JWT** guide to understand how Clerk authentication uses these configuration values
- Experiment with changing profile settings and observing behavior
- Create a new profile (e.g., `application-test.yml`) for testing

---

**Related Topics:**
- [Understanding Spring Security & JWT Authentication](./understanding-spring-security-jwt.md)
- [Understanding Dependency Injection & Spring Beans](./understanding-dependency-injection.md)
