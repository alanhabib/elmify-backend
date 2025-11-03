# Understanding RESTful API Design Patterns

## Table of Contents
1. [What is REST?](#what-is-rest)
2. [REST Principles](#rest-principles)
3. [HTTP Methods and Their Meanings](#http-methods-and-their-meanings)
4. [Resource-Based URLs](#resource-based-urls)
5. [Your Application's API Structure](#your-applications-api-structure)
6. [Request and Response Patterns](#request-and-response-patterns)
7. [Pagination for Mobile](#pagination-for-mobile)
8. [Error Handling](#error-handling)
9. [API Versioning](#api-versioning)
10. [Security Patterns](#security-patterns)
11. [Best Practices](#best-practices)

---

## What is REST?

**REST** stands for **Representational State Transfer**. It's an architectural style for designing networked applications, particularly web APIs.

### The Restaurant Analogy

Think of a REST API like a restaurant:

| Restaurant Concept | REST API Concept |
|-------------------|------------------|
| **Menu** | API Documentation (what you can order) |
| **Waiter** | HTTP Protocol (takes orders, brings food) |
| **Kitchen** | Server/Backend (prepares the food) |
| **Dishes** | Resources (users, lectures, favorites) |
| **Order** | HTTP Request (GET salad, POST new order) |
| **Meal** | HTTP Response (here's your salad) |

When you go to a restaurant:
1. You **look at the menu** (read API docs)
2. You **tell the waiter** what you want (make HTTP request)
3. The **kitchen prepares** it (server processes)
4. The **waiter brings** your food (HTTP response)

---

## REST Principles

REST is built on six core principles:

### 1. **Client-Server Architecture**

The client (React Native app) and server (Spring Boot backend) are **separate and independent**.

```
┌─────────────────┐          HTTP          ┌─────────────────┐
│  React Native   │ ←――――――――――――――――――――→ │  Spring Boot    │
│  Mobile App     │      Request/Response   │  Backend API    │
│  (Client)       │                         │  (Server)       │
└─────────────────┘                         └─────────────────┘
```

**Benefits:**
- Frontend can be rewritten (React Native → Flutter) without changing backend
- Backend can be scaled independently
- Multiple clients can use the same API (mobile app, web app, CLI)

---

### 2. **Stateless**

Each request contains **all the information** needed to process it. The server doesn't remember previous requests.

**Stateless (REST):**
```
Request 1: GET /lectures/5 with JWT token
  → Server: "Here's lecture 5"

Request 2: GET /favorites with JWT token
  → Server: "Here are favorites" (doesn't remember Request 1)
```

**Stateful (NOT REST):**
```
Request 1: POST /login with credentials
  → Server: "OK, you're user 1. I'll remember this in session."

Request 2: GET /favorites
  → Server: "You're user 1, here are your favorites"
```

**Why Stateless is Better:**
- Easier to scale (any server can handle any request)
- More reliable (no session data to corrupt)
- Better for mobile (handles network interruptions)

**How Your App Achieves This:**
- Every request includes a JWT token
- Server doesn't store session data
- All user context comes from the token

---

### 3. **Cacheable**

Responses should indicate whether they can be cached to improve performance.

```java
// Your StorageService generates presigned URLs with expiration
public String generatePresignedUrl(String objectKey) {
    GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
        .signatureDuration(presignedUrlExpiration)  // 1 hour
        .getObjectRequest(getObjectRequest)
        .build();
    // This URL can be cached for 1 hour
}
```

**Cache Headers in Responses:**
```
HTTP/1.1 200 OK
Cache-Control: public, max-age=3600
ETag: "abc123"

{
  "url": "https://storage.example.com/lecture.mp3?expires=..."
}
```

---

### 4. **Uniform Interface**

All resources follow **consistent patterns** for accessing and manipulating them.

```
GET    /api/v1/lectures      ← Get all lectures
GET    /api/v1/lectures/5    ← Get lecture 5
POST   /api/v1/lectures      ← Create new lecture

GET    /api/v1/favorites     ← Get all favorites (same pattern!)
POST   /api/v1/favorites     ← Add a favorite
DELETE /api/v1/favorites/5   ← Remove favorite 5
```

**Consistency** means developers can guess how your API works.

---

### 5. **Layered System**

Clients can't tell if they're connected directly to the server or through intermediaries.

```
Mobile App
    ↓
Load Balancer (AWS ALB)
    ↓
API Gateway
    ↓
Spring Boot Application (your code)
    ↓
PostgreSQL Database
```

Your React Native app doesn't know (or care) about these layers. It just makes requests to `https://api.audibleclone.com`.

---

### 6. **Code on Demand (Optional)**

Server can send executable code (like JavaScript) to clients.

**Example:** Your API could return JavaScript for analytics:
```json
{
  "lecture": {...},
  "analytics_script": "function trackPlay() { ... }"
}
```

This principle is **optional** and rarely used in modern APIs.

---

## HTTP Methods and Their Meanings

REST uses HTTP methods (verbs) to indicate the **type of operation**.

### The CRUD Mapping

| HTTP Method | CRUD Operation | SQL Equivalent | Idempotent? | Safe? |
|-------------|----------------|----------------|-------------|-------|
| **GET**     | Read           | SELECT         | Yes ✓       | Yes ✓ |
| **POST**    | Create         | INSERT         | No ✗        | No ✗  |
| **PUT**     | Update (full)  | UPDATE         | Yes ✓       | No ✗  |
| **PATCH**   | Update (partial)| UPDATE        | No ✗        | No ✗  |
| **DELETE**  | Delete         | DELETE         | Yes ✓       | No ✗  |

**Idempotent** = Making the same request multiple times has the same effect as making it once.

**Safe** = The request doesn't modify any data.

---

### GET - Retrieve Resources

**Purpose:** Fetch data without modifying anything.

**Your Examples:**

```java
// Get all lectures (paginated)
@GetMapping
public ResponseEntity<PagedResponse<LectureDto>> getAllLectures(Pageable pageable) {
    // Returns: {content: [...], page: 0, size: 20, totalPages: 5}
}

// Get single lecture by ID
@GetMapping("/{id}")
public ResponseEntity<LectureDto> getLectureById(@PathVariable Long id) {
    // Returns: {id: 5, title: "...", speakerId: 2, speakerName: "..."}
}

// Get lectures by collection
@GetMapping("/collection/{collectionId}")
public ResponseEntity<PagedResponse<LectureDto>> getLecturesByCollection(
    @PathVariable Long collectionId, Pageable pageable) {
    // Returns: {content: [...], ...}
}
```

**HTTP Request:**
```http
GET /api/v1/lectures/5 HTTP/1.1
Host: api.audibleclone.com
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

**HTTP Response:**
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": 5,
  "title": "Introduction to Philosophy",
  "speakerId": 2,
  "speakerName": "Dr. Smith",
  "duration": 3600,
  "thumbnailUrl": "https://storage.example.com/thumb.jpg?expires=..."
}
```

**Rules:**
- ✅ Should not modify data
- ✅ Can be cached
- ✅ Can be bookmarked
- ✅ Safe to retry

---

### POST - Create New Resources

**Purpose:** Create a new resource.

**Your Examples:**

```java
// Add a lecture to favorites
@PostMapping
public ResponseEntity<?> addFavorite(
    @AuthenticationPrincipal Jwt jwt,
    @RequestParam Long lectureId) {

    String clerkId = jwt.getSubject();
    favoriteService.addFavorite(clerkId, lectureId);

    return ResponseEntity.ok(Map.of("message", "Added to favorites"));
}

// Sync user from Clerk
@PostMapping("/sync")
public ResponseEntity<UserDto> syncUser(@RequestBody UserSyncRequest request) {
    User user = userService.syncUserFromClerk(request);
    return ResponseEntity.ok(UserDto.fromEntity(user));
}
```

**HTTP Request:**
```http
POST /api/v1/favorites HTTP/1.1
Host: api.audibleclone.com
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
Content-Type: application/json

{
  "lectureId": 5
}
```

**HTTP Response:**
```http
HTTP/1.1 201 Created
Location: /api/v1/favorites/12
Content-Type: application/json

{
  "id": 12,
  "userId": 1,
  "lectureId": 5,
  "createdAt": "2025-01-20T14:30:00Z"
}
```

**Rules:**
- ✅ Creates a new resource
- ✅ Returns 201 Created (or 200 OK)
- ✅ Should include `Location` header with new resource URL
- ❌ **NOT idempotent** (calling twice creates two favorites... unless you have unique constraint)

---

### PUT - Replace Entire Resource

**Purpose:** Replace an entire resource with new data.

**Example (hypothetical):**

```java
// Replace entire user profile
@PutMapping("/{id}")
public ResponseEntity<UserDto> updateUser(
    @PathVariable Long id,
    @RequestBody UserDto userDto) {

    User user = userService.replaceUser(id, userDto);
    return ResponseEntity.ok(UserDto.fromEntity(user));
}
```

**HTTP Request:**
```http
PUT /api/v1/users/1 HTTP/1.1
Content-Type: application/json

{
  "clerkId": "user_123",
  "email": "new@example.com",
  "displayName": "New Name",
  "profileImageUrl": "https://...",
  "isPremium": true
}
```

**Rules:**
- ✅ Replaces the **entire** resource
- ✅ Idempotent (same request twice = same result)
- ✅ All fields must be provided (missing fields get deleted/set to null)

---

### PATCH - Partial Update

**Purpose:** Update only specific fields of a resource.

**Example:**

```java
// Update playback position (partial update)
@PatchMapping("/{lectureId}")
public ResponseEntity<PlaybackPositionDto> updatePosition(
    @AuthenticationPrincipal Jwt jwt,
    @PathVariable Long lectureId,
    @RequestBody UpdatePositionRequest request) {

    PlaybackPosition position = playbackPositionService.updatePosition(
        jwt.getSubject(), lectureId, request.getCurrentPosition()
    );
    return ResponseEntity.ok(PlaybackPositionDto.fromEntity(position));
}
```

**HTTP Request:**
```http
PATCH /api/v1/playback/5 HTTP/1.1
Content-Type: application/json

{
  "currentPosition": 1234
}
```

Only `currentPosition` is updated; all other fields remain unchanged.

**Rules:**
- ✅ Updates only the provided fields
- ✅ Other fields remain unchanged
- ❌ **NOT idempotent** (depends on implementation)

---

### DELETE - Remove Resource

**Purpose:** Delete a resource.

**Your Examples:**

```java
// Remove from favorites
@DeleteMapping("/{lectureId}")
public ResponseEntity<?> removeFavorite(
    @AuthenticationPrincipal Jwt jwt,
    @PathVariable Long lectureId) {

    favoriteService.removeFavorite(jwt.getSubject(), lectureId);
    return ResponseEntity.ok(Map.of("message", "Removed from favorites"));
}

// Delete playback position
@DeleteMapping("/{lectureId}")
public ResponseEntity<?> deletePosition(
    @AuthenticationPrincipal Jwt jwt,
    @PathVariable Long lectureId) {

    playbackPositionService.deletePosition(jwt.getSubject(), lectureId);
    return ResponseEntity.noContent().build();
}
```

**HTTP Request:**
```http
DELETE /api/v1/favorites/5 HTTP/1.1
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

**HTTP Response:**
```http
HTTP/1.1 204 No Content
```

Or:

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "message": "Removed from favorites"
}
```

**Rules:**
- ✅ Deletes the resource
- ✅ Returns 204 No Content (or 200 OK with message)
- ✅ **Idempotent** (deleting twice has same effect as once)

---

## Resource-Based URLs

REST APIs are designed around **resources** (nouns), not actions (verbs).

### ❌ Wrong (Action-Based URLs)

```
POST /api/v1/createUser
POST /api/v1/getFavorites
POST /api/v1/addFavorite
POST /api/v1/deleteFavorite
```

Problems:
- Everything is POST (loses HTTP method semantics)
- URLs are verbs (actions) instead of nouns (resources)
- Inconsistent and unpredictable

---

### ✅ Correct (Resource-Based URLs)

```
POST   /api/v1/users           ← Create user
GET    /api/v1/users/1         ← Get user 1

GET    /api/v1/favorites       ← List favorites
POST   /api/v1/favorites       ← Add favorite
DELETE /api/v1/favorites/5     ← Remove favorite 5
```

**Rules:**
- Use **nouns** for resources (users, lectures, favorites)
- Use **HTTP methods** for actions (GET, POST, DELETE)
- Use **plural** names (`/users`, not `/user`)

---

### URL Hierarchy (Nested Resources)

When resources are related, nest them:

```
GET /api/v1/speakers/2                    ← Get speaker 2
GET /api/v1/speakers/2/lectures           ← Get lectures by speaker 2

GET /api/v1/collections/10                ← Get collection 10
GET /api/v1/collections/10/lectures       ← Get lectures in collection 10

GET /api/v1/users/1                       ← Get user 1
GET /api/v1/users/1/favorites             ← Get user 1's favorites
```

**Your Implementation:**

```java
@GetMapping("/speaker/{speakerId}")
public ResponseEntity<PagedResponse<LectureDto>> getLecturesBySpeaker(
    @PathVariable Long speakerId, Pageable pageable) {
    // Returns lectures for a specific speaker
}

@GetMapping("/collection/{collectionId}")
public ResponseEntity<PagedResponse<LectureDto>> getLecturesByCollection(
    @PathVariable Long collectionId, Pageable pageable) {
    // Returns lectures for a specific collection
}
```

---

### Query Parameters for Filtering and Pagination

Use query parameters for **optional features**:

```
GET /api/v1/lectures?page=0&size=20               ← Pagination
GET /api/v1/lectures?search=philosophy            ← Search
GET /api/v1/lectures?speaker=Dr.Smith&sort=title  ← Filter and sort
```

**Your Implementation:**

```java
@GetMapping
public ResponseEntity<PagedResponse<LectureDto>> getAllLectures(Pageable pageable) {
    // Pageable automatically extracts: ?page=0&size=20&sort=title,asc
}
```

**Spring automatically parses:**
- `?page=0` → `pageable.getPageNumber() = 0`
- `?size=20` → `pageable.getPageSize() = 20`
- `?sort=title,asc` → `pageable.getSort() = Sort.by("title").ascending()`

---

## Your Application's API Structure

### API Versioning

All your endpoints start with `/api/v1/`:

```
/api/v1/lectures
/api/v1/collections
/api/v1/speakers
/api/v1/favorites
/api/v1/playback
```

**Why `/v1/`?**
- Allows future breaking changes (`/api/v2/`)
- Clients can continue using v1 while migrating to v2

---

### Endpoint Inventory

#### **Lectures API** (`/api/v1/lectures`)

| Method | Endpoint | Purpose | Auth Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/lectures` | List all lectures (paginated) | No |
| GET | `/api/v1/lectures/{id}` | Get single lecture | No |
| GET | `/api/v1/lectures/collection/{id}` | Get lectures by collection | No |
| GET | `/api/v1/lectures/speaker/{id}` | Get lectures by speaker | No |
| GET | `/api/v1/lectures/{id}/stream-url` | Get secure stream URL | **Yes** |

**Design Notes:**
- Browsing is **public** (no auth required)
- Streaming is **protected** (requires JWT)

---

#### **Collections API** (`/api/v1/collections`)

| Method | Endpoint | Purpose | Auth Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/collections` | List all collections | **Yes** |
| GET | `/api/v1/collections/{id}` | Get single collection | **Yes** |

---

#### **Speakers API** (`/api/v1/speakers`)

| Method | Endpoint | Purpose | Auth Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/speakers` | List all speakers | No |
| GET | `/api/v1/speakers/{id}` | Get single speaker | No |

---

#### **Favorites API** (`/api/v1/favorites`)

| Method | Endpoint | Purpose | Auth Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/favorites` | Get user's favorites | **Yes** |
| GET | `/api/v1/favorites/check/{lectureId}` | Check if favorited | **Yes** |
| POST | `/api/v1/favorites` | Add to favorites | **Yes** |
| DELETE | `/api/v1/favorites/{lectureId}` | Remove from favorites | **Yes** |
| GET | `/api/v1/favorites/count` | Count user's favorites | **Yes** |

---

#### **Playback Positions API** (`/api/v1/playback`)

| Method | Endpoint | Purpose | Auth Required |
|--------|----------|---------|---------------|
| GET | `/api/v1/playback/{lectureId}` | Get playback position | **Yes** |
| POST | `/api/v1/playback/{lectureId}` | Update position | **Yes** |
| DELETE | `/api/v1/playback/{lectureId}` | Delete position | **Yes** |
| GET | `/api/v1/playback` | Get all positions | **Yes** |
| GET | `/api/v1/playback/continue-listening` | Get recent playback | **Yes** |
| GET | `/api/v1/playback/check/{lectureId}` | Check if has position | **Yes** |

---

## Request and Response Patterns

### Standard Response Structure

#### Success Response (Single Object)

```json
{
  "id": 5,
  "title": "Introduction to Philosophy",
  "speakerId": 2,
  "speakerName": "Dr. Smith",
  "collectionId": 10,
  "collectionTitle": "Philosophy 101",
  "duration": 3600,
  "thumbnailUrl": "https://..."
}
```

#### Success Response (List - Paginated)

```json
{
  "content": [
    { "id": 1, "title": "Lecture 1", ... },
    { "id": 2, "title": "Lecture 2", ... }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 156,
  "totalPages": 8,
  "first": true,
  "last": false
}
```

**Your Implementation:**

```java
public record PagedResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }
}
```

This format is **perfect for mobile apps** - it provides all the info needed for infinite scroll or pagination UI.

---

### Error Response Structure

#### Standard Error Format

```json
{
  "timestamp": "2025-01-20T14:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Lecture not found with id: 999",
  "path": "/api/v1/lectures/999"
}
```

**Your Exception Handling:**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
        ResourceNotFoundException ex, WebRequest request) {

        ErrorResponse error = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getDescription(false)
        );

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
}
```

---

### HTTP Status Codes

Your API uses appropriate status codes:

#### Success Codes (2xx)

| Code | Meaning | When to Use |
|------|---------|-------------|
| **200 OK** | Success | GET, POST, DELETE with response body |
| **201 Created** | Resource created | POST when creating new resource |
| **204 No Content** | Success, no response body | DELETE with no message |

#### Client Error Codes (4xx)

| Code | Meaning | When to Use |
|------|---------|-------------|
| **400 Bad Request** | Invalid input | Validation failures |
| **401 Unauthorized** | Not authenticated | Missing or invalid JWT |
| **403 Forbidden** | Not authorized | Valid JWT but insufficient permissions |
| **404 Not Found** | Resource doesn't exist | Lecture/user/favorite not found |
| **409 Conflict** | Duplicate resource | Adding same favorite twice |

#### Server Error Codes (5xx)

| Code | Meaning | When to Use |
|------|---------|-------------|
| **500 Internal Server Error** | Server crashed | Unhandled exceptions |
| **503 Service Unavailable** | Server overloaded | Database down, too much traffic |

**Your Examples:**

```java
// 200 OK
return ResponseEntity.ok(lectureDto);

// 201 Created
return ResponseEntity.status(HttpStatus.CREATED).body(userDto);

// 204 No Content
return ResponseEntity.noContent().build();

// 404 Not Found
return lectureService.getLectureById(id)
    .map(lecture -> ResponseEntity.ok(LectureDto.fromEntity(lecture, storageService)))
    .orElseThrow(() -> new ResourceNotFoundException("Lecture", id));

// 401 Unauthorized (handled by Spring Security)
@PreAuthorize("isAuthenticated()")
public ResponseEntity<?> getStreamUrl(@PathVariable Long id) { ... }
```

---

## Pagination for Mobile

Pagination is **critical** for mobile apps to:
- Reduce data transfer (save user's bandwidth)
- Improve performance (load only what's needed)
- Enable infinite scroll UX

### Your Pagination Implementation

```java
@GetMapping
public ResponseEntity<PagedResponse<LectureDto>> getAllLectures(Pageable pageable) {
    Page<LectureDto> lectureDtos = lectureService.getAllLectures(pageable)
        .map(lecture -> LectureDto.fromEntity(lecture, storageService));
    PagedResponse<LectureDto> response = PagedResponse.from(lectureDtos);
    return ResponseEntity.ok(response);
}
```

**How It Works:**

1. **Client Request:**
   ```http
   GET /api/v1/lectures?page=0&size=20&sort=title,asc
   ```

2. **Spring Parses Parameters:**
   - `page=0` → First page (0-indexed)
   - `size=20` → 20 items per page
   - `sort=title,asc` → Sort by title ascending

3. **Service Layer:**
   ```java
   Page<Lecture> lectures = lectureRepository.findAll(pageable);
   // SQL: SELECT * FROM lectures ORDER BY title ASC LIMIT 20 OFFSET 0
   ```

4. **Response:**
   ```json
   {
     "content": [ /* 20 lectures */ ],
     "page": 0,
     "size": 20,
     "totalElements": 156,
     "totalPages": 8,
     "first": true,
     "last": false
   }
   ```

5. **Mobile App Logic:**
   ```typescript
   // Load first page
   const response = await fetch('/api/v1/lectures?page=0&size=20');

   // User scrolls down... load next page
   const nextPage = await fetch('/api/v1/lectures?page=1&size=20');

   // Continue until last = true
   ```

---

### Pagination Metadata

```json
{
  "page": 2,           // Current page (0-indexed)
  "size": 20,          // Items per page
  "totalElements": 156,// Total items across all pages
  "totalPages": 8,     // Total pages
  "first": false,      // Is this the first page?
  "last": false        // Is this the last page?
}
```

**Mobile App Can Use This To:**
- Show "Page 3 of 8"
- Disable "Previous" button when `first = true`
- Disable "Next" button when `last = true`
- Show loading indicator until all pages loaded
- Calculate scroll progress: `(page + 1) / totalPages * 100%`

---

## Error Handling

### Exception Hierarchy

```java
// Custom exception
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceName, Long id) {
        super(String.format("%s not found with id: %d", resourceName, id));
    }
}

// Global handler
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
        ResourceNotFoundException ex) {

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
        MethodArgumentNotValidException ex) {

        List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.toList());

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("Validation failed", errors));
    }
}
```

### Validation Errors

```java
public record UpdatePositionRequest(
    @Min(0) @NotNull
    Integer currentPosition
) {}

@PostMapping("/{lectureId}")
public ResponseEntity<?> updatePosition(
    @Valid @RequestBody UpdatePositionRequest request) {
    // @Valid triggers validation
}
```

**Invalid Request:**
```http
POST /api/v1/playback/5
Content-Type: application/json

{
  "currentPosition": -10
}
```

**Error Response:**
```json
{
  "timestamp": "2025-01-20T14:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    "currentPosition: must be greater than or equal to 0"
  ]
}
```

---

## API Versioning

### Why Version APIs?

As your app evolves, you may need to make **breaking changes**:

```
Version 1 (Current):
GET /api/v1/lectures → Returns { id, title, speakerId, speakerName }

Version 2 (Future):
GET /api/v2/lectures → Returns { id, title, speaker: { id, name, bio } }
```

**Without versioning:**
- Old mobile apps break when you deploy v2
- Users must update their app immediately
- No gradual migration

**With versioning:**
- v1 continues to work for old apps
- New apps use v2
- Gradual migration over months

---

### Versioning Strategies

#### 1. URL Versioning (Your Approach) ✅

```
/api/v1/lectures
/api/v2/lectures
```

**Pros:**
- Clear and explicit
- Easy to test both versions
- Works with all clients

**Cons:**
- URL pollution

---

#### 2. Header Versioning

```http
GET /api/lectures
Accept: application/vnd.audibleclone.v1+json
```

**Pros:**
- Clean URLs

**Cons:**
- Harder to test (must set headers)
- Not visible in browser

---

#### 3. Query Parameter Versioning

```
GET /api/lectures?version=1
GET /api/lectures?version=2
```

**Pros:**
- Clean URLs
- Easy to test

**Cons:**
- Mixes versioning with other params

---

### Your Implementation

```java
@RestController
@RequestMapping("/api/v1/lectures")  // ← Version in URL
public class LectureController {
    // All endpoints are /api/v1/lectures/*
}
```

When you need v2:

```java
@RestController
@RequestMapping("/api/v2/lectures")
public class LectureControllerV2 {
    // New behavior, different response structure
}
```

Both v1 and v2 run simultaneously. Deprecate v1 after 6-12 months.

---

## Security Patterns

### Authentication with JWT

**Your Pattern:**

```java
@GetMapping("/{id}/stream-url")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<Map<String, String>> getStreamUrl(
    @AuthenticationPrincipal Jwt jwt,  // Spring extracts JWT
    @PathVariable Long id) {

    String clerkId = jwt.getSubject();  // Get user ID from token
    // ...
}
```

**Request:**
```http
GET /api/v1/lectures/5/stream-url
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Security Flow:**

```
1. Client sends JWT in Authorization header
      ↓
2. Spring Security intercepts request
      ↓
3. ClerkJwtDecoder validates JWT signature
      ↓
4. JWT is valid → Extract user info (clerkId, email, roles)
      ↓
5. @PreAuthorize checks if authenticated
      ↓
6. Controller receives authenticated Jwt object
      ↓
7. Service layer uses clerkId to look up user
```

---

### Public vs Protected Endpoints

```java
// SecurityConfig.java
.authorizeHttpRequests(authorize -> authorize
    .requestMatchers("/actuator/health/**").permitAll()
    .requestMatchers("/api/v1/users/sync").permitAll()

    // Browsing is PUBLIC
    .requestMatchers(HttpMethod.GET, "/api/v1/lectures").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/v1/lectures/{id}").permitAll()

    // Everything else requires authentication
    .requestMatchers("/api/v1/**").authenticated()
    .anyRequest().denyAll()
)
```

**Design Philosophy:**
- **Browsing = Public** (anyone can see lectures, speakers, collections)
- **Interaction = Protected** (favorites, playback, streaming requires login)

---

### CORS for React Native

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of("http://localhost:8081"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
}
```

**Why Needed?**
- React Native apps make requests from `http://localhost:8081` (dev)
- Without CORS, browser blocks the request
- Production: Update `allowedOrigins` to your deployed app URL

---

## Best Practices

### 1. Use DTOs, Never Return Entities

❌ **WRONG:**
```java
@GetMapping("/{id}")
public ResponseEntity<Lecture> getLecture(@PathVariable Long id) {
    Lecture lecture = lectureService.findById(id);
    return ResponseEntity.ok(lecture);  // Exposes internal structure!
}
```

Problems:
- Exposes all entity fields (even sensitive ones)
- Causes lazy loading exceptions
- Couples API to database schema

✅ **CORRECT:**
```java
@GetMapping("/{id}")
public ResponseEntity<LectureDto> getLecture(@PathVariable Long id) {
    Lecture lecture = lectureService.findById(id);
    return ResponseEntity.ok(LectureDto.fromEntity(lecture, storageService));
}
```

---

### 2. Use Meaningful HTTP Status Codes

```java
// Create
@PostMapping
public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto) {
    User user = userService.create(userDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(UserDto.fromEntity(user));
}

// Not found
@GetMapping("/{id}")
public ResponseEntity<LectureDto> getLecture(@PathVariable Long id) {
    return lectureService.findById(id)
        .map(lecture -> ResponseEntity.ok(LectureDto.fromEntity(lecture, storageService)))
        .orElse(ResponseEntity.notFound().build());
}

// No content
@DeleteMapping("/{id}")
public ResponseEntity<Void> deletePosition(@PathVariable Long id) {
    playbackPositionService.delete(id);
    return ResponseEntity.noContent().build();
}
```

---

### 3. Validate Input

```java
public record CreateLectureRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title too long")
    String title,

    @NotNull(message = "Speaker ID is required")
    @Positive(message = "Speaker ID must be positive")
    Long speakerId,

    @Min(value = 0, message = "Duration cannot be negative")
    Integer duration
) {}

@PostMapping
public ResponseEntity<LectureDto> createLecture(
    @Valid @RequestBody CreateLectureRequest request) {
    // @Valid ensures validation runs before method executes
}
```

---

### 4. Paginate Large Collections

❌ **WRONG:**
```java
@GetMapping
public List<Lecture> getAllLectures() {
    return lectureRepository.findAll();  // Returns 10,000 lectures!
}
```

✅ **CORRECT:**
```java
@GetMapping
public ResponseEntity<PagedResponse<LectureDto>> getAllLectures(Pageable pageable) {
    Page<Lecture> lectures = lectureRepository.findAll(pageable);
    return ResponseEntity.ok(PagedResponse.from(
        lectures.map(l -> LectureDto.fromEntity(l, storageService))
    ));
}
```

---

### 5. Use Consistent Naming

```
✅ /api/v1/lectures          (plural)
✅ /api/v1/favorites         (plural)
✅ /api/v1/playback          (singular, represents user's playback state)

❌ /api/v1/lecture           (inconsistent)
❌ /api/v1/get-favorites     (verb in URL)
❌ /api/v1/playback_position (underscores)
```

---

### 6. Document Your API

Use Swagger/OpenAPI annotations:

```java
@Operation(summary = "Get Lecture by ID",
           description = "Retrieves a specific lecture by its ID. This is a public endpoint.")
@ApiResponse(responseCode = "200", description = "Successfully retrieved lecture")
@ApiResponse(responseCode = "404", description = "Lecture not found")
@GetMapping("/{id}")
public ResponseEntity<LectureDto> getLectureById(
    @Parameter(description = "Lecture ID", example = "1")
    @PathVariable Long id) {
    // ...
}
```

This generates interactive docs at `/swagger-ui/index.html`.

---

## Key Takeaways

### What You Learned

1. **REST is About Resources**
   - URLs represent resources (nouns), not actions (verbs)
   - HTTP methods indicate operations (GET, POST, PUT, DELETE)

2. **Your API Structure**
   - `/api/v1/` for versioning
   - Resource-based URLs (`/lectures`, `/favorites`)
   - Nested resources (`/speakers/{id}/lectures`)

3. **HTTP Methods Map to CRUD**
   - GET → Read (safe, idempotent)
   - POST → Create (not idempotent)
   - PUT → Replace (idempotent)
   - PATCH → Update (not idempotent)
   - DELETE → Remove (idempotent)

4. **Pagination for Mobile**
   - Essential for performance and UX
   - Use `PagedResponse` for consistent format
   - Include metadata (page, size, totalPages, etc.)

5. **Security Patterns**
   - Public browsing, protected interactions
   - JWT authentication with `@PreAuthorize`
   - CORS for React Native

6. **Best Practices**
   - Always use DTOs
   - Meaningful HTTP status codes
   - Validate all input
   - Paginate large collections
   - Version your API

### How This Connects to Your Application

```
React Native App
      ↓ HTTP Requests
RESTful API Controllers (@RestController)
      ↓ Method Calls
Service Layer (@Service)
      ↓ JPA Queries
Repository Layer (@Repository)
      ↓ SQL Queries
PostgreSQL Database
```

### Next Steps

- [Understanding DTOs and the DTO Pattern](./understanding-dtos-pattern.md) - How to transform entities to API responses
- [Understanding Spring Security & JWT Authentication](./understanding-spring-security-jwt.md) - How authentication works
- [Understanding Dependency Injection](./understanding-dependency-injection.md) - How Spring wires everything together

---

**Congratulations!** You now understand how RESTful APIs work and how your AudibleClone backend follows REST principles for clean, predictable API design.
