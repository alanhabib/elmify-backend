# Understanding DTOs and the DTO Pattern

## Table of Contents
1. [What is a DTO?](#what-is-a-dto)
2. [The Problem DTOs Solve](#the-problem-dtos-solve)
3. [Entity vs DTO](#entity-vs-dto)
4. [Java Records for DTOs](#java-records-for-dtos)
5. [Your Application's DTO Pattern](#your-applications-dto-pattern)
6. [LectureDto Deep Dive](#lecturedto-deep-dive)
7. [Entity-to-DTO Conversion](#entity-to-dto-conversion)
8. [DTO-to-Entity Conversion](#dto-to-entity-conversion)
9. [Flattening Relationships](#flattening-relationships)
10. [Data Transformation in DTOs](#data-transformation-in-dtos)
11. [Validation with DTOs](#validation-with-dtos)
12. [Common DTO Patterns](#common-dto-patterns)
13. [Best Practices](#best-practices)

---

## What is a DTO?

**DTO** stands for **Data Transfer Object**.

### Simple Definition
A DTO is a simple object that carries data between processes. It's specifically designed for transferring data (usually over a network) without any business logic.

### Analogy
Think of DTOs like **packaging**:

```
Entity (Lecture) = Raw product in warehouse
    ↓ (package it safely)
DTO (LectureDto) = Product in shipping box
    ↓ (ship to customer)
API Response = Customer receives it
```

**Just like shipping:**
- You don't ship the raw warehouse inventory (Entity)
- You package it appropriately (DTO)
- You might repackage based on destination (different DTOs for different use cases)
- The package doesn't include warehouse-internal info (private/sensitive data)

---

## The Problem DTOs Solve

### Problem 1: Exposing Entities Directly is Dangerous

**Without DTOs (Bad Approach):**
```java
@RestController
public class LectureController {
    @GetMapping("/api/v1/lectures/{id}")
    public Lecture getLecture(@PathVariable Long id) {
        return lectureRepository.findById(id).get();  // ❌ Returns entire entity!
    }
}
```

**What happens:**
```json
// API Response - exposes too much!
{
  "id": 1,
  "title": "Introduction to Spring",
  "filePath": "lectures/speaker-1/intro.mp3",  // ❌ Internal path exposed
  "fileHash": "sha256:abc123...",              // ❌ Internal hash exposed
  "isPublic": false,                            // ❌ Internal flag exposed
  "speaker": {
    "id": 5,
    "name": "John Doe",
    "email": "john@example.com",               // ❌ Personal email exposed!
    "lectures": [                               // ❌ Circular reference!
      { "id": 1, "speaker": { "id": 5, ... } }  // 💥 Infinite loop!
    ]
  },
  "collection": {
    "id": 10,
    "lectures": [ ... ]                         // ❌ Loads all lectures!
  }
}
```

**Problems:**
1. ❌ Exposes internal/sensitive data
2. ❌ Circular references cause infinite loops or errors
3. ❌ Lazy loading exceptions when accessing relationships
4. ❌ Sends too much data (performance issue)
5. ❌ Couples API to database schema (hard to change)

---

### Problem 2: Lazy Loading Exceptions

**Without DTOs:**
```java
@GetMapping("/api/v1/lectures/{id}")
public Lecture getLecture(@PathVariable Long id) {
    Lecture lecture = lectureRepository.findById(id).get();
    return lecture;  // ❌ Transaction ends here
}
```

**When serializing to JSON:**
```
org.hibernate.LazyInitializationException:
failed to lazily initialize a collection of role:
com.audibleclone.backend.entity.Lecture.speaker,
could not initialize proxy - no Session
```

**Why?**
```
1. Controller method finishes (transaction closes)
2. Jackson tries to serialize Lecture to JSON
3. Jackson accesses lecture.getSpeaker()
4. Speaker is @ManyToOne(fetch = LAZY)
5. Hibernate tries to fetch Speaker
6. But transaction is already closed!
7. 💥 LazyInitializationException
```

---

### Problem 3: Over-fetching Data

**Without DTOs:**
```java
// Client only needs: id, title, speakerName
// But entity loads everything:
{
  "id": 1,
  "title": "...",
  "genre": "...",
  "year": 2023,
  "duration": 3600,
  "fileName": "...",
  "filePath": "...",
  "fileSize": 1048576,
  "fileFormat": "mp3",
  "bitrate": 320,
  "sampleRate": 44100,
  "fileHash": "...",
  "thumbnailUrl": "...",
  "waveformData": "...",  // 50KB of JSON data!
  "description": "...",   // 10KB of text
  // ... and more
}
```

**Result:**
- Mobile app downloads 100KB per lecture
- Listing 50 lectures = 5MB of data
- Slow network performance
- Expensive data usage for users

---

### Solution: DTOs

**With DTOs:**
```java
@RestController
public class LectureController {
    @GetMapping("/api/v1/lectures/{id}")
    public LectureDto getLecture(@PathVariable Long id) {
        Lecture lecture = lectureService.findById(id);
        return LectureDto.fromEntity(lecture, storageService);  // ✅ Safe conversion
    }
}
```

**Result:**
```json
{
  "id": 1,
  "title": "Introduction to Spring",
  "duration": 3600,
  "speakerId": 5,           // ✅ Just the ID
  "speakerName": "John Doe",  // ✅ Just the name
  "collectionId": 10,
  "collectionTitle": "Spring Boot Fundamentals",
  "thumbnailUrl": "https://presigned-url.com/...",  // ✅ Secure presigned URL
  "createdAt": "2025-01-15T10:30:00Z"
}
```

**Benefits:**
- ✅ Only exposes what client needs
- ✅ No circular references
- ✅ No lazy loading exceptions
- ✅ Smaller payload (better performance)
- ✅ API independent of database schema

---

## Entity vs DTO

### Side-by-Side Comparison

| Aspect | Entity (Lecture) | DTO (LectureDto) |
|--------|------------------|------------------|
| **Purpose** | Represents database table | Represents API data |
| **Location** | `entity/` package | `dto/` package |
| **Annotations** | `@Entity`, `@Table`, `@ManyToOne` | `@NotBlank`, `@Size` (validation) |
| **Relationships** | `Speaker speaker`, `Collection collection` | `Long speakerId`, `String speakerName` |
| **Mutability** | Mutable (setters) | Immutable (Java Record) |
| **Business Logic** | Can have methods (`getFormattedDuration()`) | Pure data (no logic) |
| **Hibernate** | Managed by Hibernate | Not managed (plain object) |
| **Lifecycle** | Tracked in persistence context | Created on-demand for API |
| **Serialization** | ❌ Should NOT be serialized to JSON | ✅ Designed for JSON serialization |

---

### Visual Flow

```
┌─────────────────────────────────────────────────────────────┐
│                      DATABASE                                │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐   │
│  │   lectures   │   │   speakers   │   │  collections │   │
│  │──────────────│   │──────────────│   │──────────────│   │
│  │ id           │   │ id           │   │ id           │   │
│  │ title        │   │ name         │   │ title        │   │
│  │ speaker_id ──┼───┤ email        │   │ cover_url    │   │
│  │ collection_id┼───┼──────────────┼───┤ ...          │   │
│  └──────────────┘   └──────────────┘   └──────────────┘   │
└────────────────────────────┬────────────────────────────────┘
                             │
                             │ JPA/Hibernate
                             ↓
┌─────────────────────────────────────────────────────────────┐
│                  JAVA APPLICATION                            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Lecture Entity                                        │  │
│  │───────────────────────────────────────────────────────│  │
│  │ - Long id                                             │  │
│  │ - String title                                        │  │
│  │ - Speaker speaker        ← ManyToOne relationship    │  │
│  │ - Collection collection  ← ManyToOne relationship    │  │
│  │ - String filePath                                     │  │
│  │ - String fileHash                                     │  │
│  │ - Boolean isPublic                                    │  │
│  └──────────────────────────────────────────────────────┘  │
│                             │                                │
│                             │ LectureDto.fromEntity()        │
│                             ↓                                │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ LectureDto (Java Record)                             │  │
│  │───────────────────────────────────────────────────────│  │
│  │ - Long id                                             │  │
│  │ - String title                                        │  │
│  │ - Long speakerId         ← Flattened from speaker    │  │
│  │ - String speakerName     ← Flattened from speaker    │  │
│  │ - Long collectionId      ← Flattened from collection │  │
│  │ - String collectionTitle ← Flattened from collection │  │
│  │ - String thumbnailUrl    ← Transformed to presigned  │  │
│  │ (no filePath, fileHash, isPublic - not exposed)      │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────┬────────────────────────────────┘
                              │
                              │ JSON Serialization
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                     API RESPONSE                             │
│  {                                                           │
│    "id": 1,                                                  │
│    "title": "Introduction to Spring",                       │
│    "speakerId": 5,                                           │
│    "speakerName": "John Doe",                                │
│    "collectionId": 10,                                       │
│    "collectionTitle": "Spring Boot Fundamentals",            │
│    "thumbnailUrl": "https://presigned-url.com/..."          │
│  }                                                           │
└─────────────────────────────────────────────────────────────┘
```

---

## Java Records for DTOs

### What is a Java Record?

Java Records (introduced in Java 14, stable in Java 16) are a concise way to create immutable data carriers.

**Traditional Java Class (Verbose):**
```java
public class LectureDto {
    private final Long id;
    private final String title;
    private final Integer duration;

    public LectureDto(Long id, String title, Integer duration) {
        this.id = id;
        this.title = title;
        this.duration = duration;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public Integer getDuration() { return duration; }

    @Override
    public boolean equals(Object o) {
        // 20 lines of boilerplate...
    }

    @Override
    public int hashCode() {
        // 5 lines of boilerplate...
    }

    @Override
    public String toString() {
        // 3 lines of boilerplate...
    }
}
```

**Java Record (Concise):**
```java
public record LectureDto(
    Long id,
    String title,
    Integer duration
) {}
```

**What You Get Automatically:**
1. ✅ Private final fields
2. ✅ Constructor with all fields
3. ✅ Getters (no `get` prefix: `dto.id()` not `dto.getId()`)
4. ✅ `equals()` method
5. ✅ `hashCode()` method
6. ✅ `toString()` method
7. ✅ Immutability (can't change fields after creation)

---

### Your LectureDto as a Record

**LectureDto.java (lines 14-50):**
```java
@JsonAutoDetect(fieldVisibility = Visibility.ANY, creatorVisibility = Visibility.ANY)
public record LectureDto(
    Long id,

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title cannot exceed 500 characters")
    String title,

    String genre,
    Integer year,

    @NotNull(message = "Duration is required")
    @PositiveOrZero(message = "Duration must be non-negative")
    Integer duration,

    String fileName,
    String filePath,

    @NotNull(message = "File size is required")
    @PositiveOrZero(message = "File size must be positive")
    Long fileSize,

    String fileFormat,
    String description,
    Integer lectureNumber,
    String thumbnailUrl,

    // Flattened relationships
    Long speakerId,
    String speakerName,
    Long collectionId,
    String collectionTitle,

    // Timestamps
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    // Can still have static methods and instance methods
    private static final Logger logger = LoggerFactory.getLogger(LectureDto.class);

    public static LectureDto fromEntity(Lecture lecture) {
        // ...
    }
}
```

**Key Features:**
1. **Immutable**: Once created, fields can't change
2. **Validation**: Bean Validation annotations work on record fields
3. **Jackson Support**: `@JsonAutoDetect` ensures proper JSON serialization
4. **Static Methods**: Can add factory methods like `fromEntity()`
5. **Instance Methods**: Can add helper methods if needed

---

## Your Application's DTO Pattern

### Directory Structure

```
src/main/java/com/audibleclone/backend/
├── dto/
│   ├── ErrorResponse.java           ← Error responses
│   ├── PagedResponse.java           ← Pagination wrapper
│   ├── LectureDto.java              ← Lecture data
│   ├── SpeakerDto.java              ← Speaker data
│   ├── CollectionDto.java           ← Collection data
│   ├── UserDto.java                 ← User data
│   ├── UserSyncDto.java             ← User sync from Clerk
│   ├── FavoriteDto.java             ← Favorite data
│   └── PlaybackPositionDto.java     ← Playback position data
├── entity/
│   ├── Lecture.java                 ← JPA entity
│   ├── Speaker.java
│   ├── Collection.java
│   └── ...
```

### Naming Convention

| Entity | DTO | Purpose |
|--------|-----|---------|
| `Lecture` | `LectureDto` | API representation of lecture |
| `User` | `UserDto` | API representation of user |
| `User` | `UserSyncDto` | Clerk → Backend sync data |
| `Favorite` | `FavoriteDto` | API representation of favorite |

**Pattern:** `{EntityName}Dto` or `{EntityName}{Purpose}Dto`

---

## LectureDto Deep Dive

Let's explore `LectureDto` in detail as it demonstrates all key patterns.

### 1. Record Declaration

**LectureDto.java (lines 14-50):**
```java
public record LectureDto(
    Long id,                    // Simple field
    String title,               // With validation
    Integer duration,
    String thumbnailUrl,        // Will be transformed
    Long speakerId,            // Flattened from relationship
    String speakerName,        // Flattened from relationship
    LocalDateTime createdAt
) { }
```

**What's Happening:**
- `id`, `title`, `duration` → Direct copy from `Lecture` entity
- `thumbnailUrl` → Transformed from relative path to presigned URL
- `speakerId`, `speakerName` → Flattened from `Lecture.speaker` relationship
- `createdAt` → Timestamp (useful for clients)

---

### 2. Validation Annotations

**LectureDto.java (lines 17-34):**
```java
public record LectureDto(
    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title cannot exceed 500 characters")
    String title,

    @NotNull(message = "Duration is required")
    @PositiveOrZero(message = "Duration must be non-negative")
    Integer duration,

    @NotNull(message = "File size is required")
    @PositiveOrZero(message = "File size must be positive")
    Long fileSize
) { }
```

**How Validation Works:**

**In Controller:**
```java
@PostMapping("/api/v1/lectures")
public LectureDto createLecture(@Valid @RequestBody LectureDto lectureDto) {
    // @Valid triggers validation
    // If validation fails, returns 400 Bad Request with errors
}
```

**Invalid Request:**
```json
{
  "title": "",         // Blank - violates @NotBlank
  "duration": -100     // Negative - violates @PositiveOrZero
}
```

**Automatic Response:**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "title",
      "message": "Title is required"
    },
    {
      "field": "duration",
      "message": "Duration must be non-negative"
    }
  ]
}
```

**This is handled by** `GlobalExceptionHandler.java`!

---

## Entity-to-DTO Conversion

### The `fromEntity()` Pattern

This is the **most important pattern** in your application!

**LectureDto.java (lines 62-89):**
```java
public static LectureDto fromEntity(Lecture lecture) {
    // Null-safe extraction of relationships
    Long speakerId = (lecture.getSpeaker() != null)
        ? lecture.getSpeaker().getId()
        : null;

    String speakerName = (lecture.getSpeaker() != null)
        ? lecture.getSpeaker().getName()
        : null;

    Long collectionId = (lecture.getCollection() != null)
        ? lecture.getCollection().getId()
        : null;

    String collectionTitle = (lecture.getCollection() != null)
        ? lecture.getCollection().getTitle()
        : null;

    return new LectureDto(
        lecture.getId(),
        lecture.getTitle(),
        lecture.getGenre(),
        lecture.getYear(),
        lecture.getDuration(),
        lecture.getFileName(),
        lecture.getFilePath(),
        lecture.getFileSize(),
        lecture.getFileFormat(),
        lecture.getDescription(),
        lecture.getLectureNumber(),
        lecture.getThumbnailUrl(),
        speakerId,        // Flattened
        speakerName,      // Flattened
        collectionId,     // Flattened
        collectionTitle,  // Flattened
        lecture.getCreatedAt(),
        lecture.getUpdatedAt()
    );
}
```

**Key Points:**
1. **Static Factory Method**: `LectureDto.fromEntity(lecture)`
2. **Null Safety**: Checks if relationships exist before accessing
3. **Flattening**: Extracts only needed fields from relationships
4. **Direct Mapping**: Simple fields copied directly

---

### Usage in Service Layer

**LectureService.java:**
```java
@Service
public class LectureService {

    @Transactional(readOnly = true)
    public LectureDto findById(Long id) {
        Lecture lecture = lectureRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Lecture not found"));

        // Convert entity to DTO
        return LectureDto.fromEntity(lecture);
    }

    @Transactional(readOnly = true)
    public Page<LectureDto> findAll(Pageable pageable) {
        Page<Lecture> lectures = lectureRepository.findAll(pageable);

        // Convert each entity to DTO
        return lectures.map(LectureDto::fromEntity);
    }
}
```

**Usage in Controller:**
```java
@RestController
@RequestMapping("/api/v1/lectures")
public class LectureController {

    @GetMapping("/{id}")
    public ResponseEntity<LectureDto> getLecture(@PathVariable Long id) {
        LectureDto dto = lectureService.findById(id);
        return ResponseEntity.ok(dto);
    }
}
```

---

### Advanced: fromEntity with Storage Service

**LectureDto.java (lines 100-143):**
```java
public static LectureDto fromEntity(Lecture lecture, StorageService storageService) {
    // ... extract speaker/collection info ...

    // Determine thumbnail URL
    String thumbnailPath = lecture.getThumbnailUrl();

    // Fallback: if lecture has no thumbnail, use collection's cover image
    if ((thumbnailPath == null || thumbnailPath.trim().isEmpty())
        && lecture.getCollection() != null) {
        thumbnailPath = lecture.getCollection().getCoverImageSmallUrl();

        if (thumbnailPath == null || thumbnailPath.trim().isEmpty()) {
            thumbnailPath = lecture.getCollection().getCoverImageUrl();
        }
    }

    return new LectureDto(
        lecture.getId(),
        lecture.getTitle(),
        // ... other fields ...
        convertToPresignedUrl(thumbnailPath, storageService),  // Transform!
        // ... rest of fields ...
    );
}
```

**What This Does:**
1. **Fallback Logic**: If lecture has no thumbnail, use collection's cover
2. **Transformation**: Convert relative path to presigned URL
3. **Security**: Generates time-limited URLs (1 hour in dev, 30 min in prod)

---

### The `convertToPresignedUrl()` Helper

**LectureDto.java (lines 154-173):**
```java
private static String convertToPresignedUrl(String path, StorageService storageService) {
    if (path == null || path.trim().isEmpty()) {
        return null;  // No path, no URL
    }

    // If already a full URL, return as-is
    if (path.startsWith("http://") || path.startsWith("https://")) {
        return path;
    }

    // Convert relative path to presigned URL
    try {
        String presignedUrl = storageService.generatePresignedUrl(path);
        logger.debug("Converted path '{}' to presigned URL", path);
        return presignedUrl;
    } catch (Exception e) {
        logger.warn("Failed to generate presigned URL for path '{}': {}", path, e.getMessage());
        return null;
    }
}
```

**Example Flow:**
```
Input (from database): "thumbnails/lecture-123.jpg"
    ↓
StorageService.generatePresignedUrl()
    ↓
Output: "https://minio.local:9000/elmify-audio/thumbnails/lecture-123.jpg?X-Amz-Signature=..."
```

**Why?**
- Clients can't access MinIO/R2 directly (requires credentials)
- Presigned URLs are time-limited and secure
- No need to proxy images through backend

---

## DTO-to-Entity Conversion

### The `toEntity()` Pattern

**LectureDto.java (lines 183-199):**
```java
public Lecture toEntity() {
    Lecture lecture = new Lecture();
    lecture.setId(this.id);
    lecture.setTitle(this.title);
    lecture.setGenre(this.genre);
    lecture.setYear(this.year);
    lecture.setDuration(this.duration);
    lecture.setFileName(this.fileName);
    lecture.setFilePath(this.filePath);
    lecture.setFileSize(this.fileSize);
    lecture.setFileFormat(this.fileFormat);
    lecture.setDescription(this.description);
    lecture.setLectureNumber(this.lectureNumber);
    lecture.setThumbnailUrl(this.thumbnailUrl);
    // Note: speaker and collection are intentionally left null here.
    return lecture;
}
```

**Important:** This only sets primitive fields. Relationships must be handled in the service layer!

---

### Usage in Service Layer

**LectureService.java:**
```java
@Transactional
public LectureDto createLecture(LectureDto lectureDto) {
    // Convert DTO to entity
    Lecture lecture = lectureDto.toEntity();

    // Manually handle relationships (DTO only has IDs)
    if (lectureDto.speakerId() != null) {
        Speaker speaker = speakerRepository.findById(lectureDto.speakerId())
            .orElseThrow(() -> new RuntimeException("Speaker not found"));
        lecture.setSpeaker(speaker);
    }

    if (lectureDto.collectionId() != null) {
        Collection collection = collectionRepository.findById(lectureDto.collectionId())
            .orElseThrow(() -> new RuntimeException("Collection not found"));
        lecture.setCollection(collection);
    }

    // Save entity
    Lecture savedLecture = lectureRepository.save(lecture);

    // Convert back to DTO for response
    return LectureDto.fromEntity(savedLecture);
}
```

---

## Flattening Relationships

### The Problem

**Entity Structure (Nested):**
```java
Lecture {
    id: 1,
    title: "Intro to Spring",
    speaker: Speaker {
        id: 5,
        name: "John Doe",
        email: "john@example.com",
        bio: "...",
        profileImageUrl: "..."
    },
    collection: Collection {
        id: 10,
        title: "Spring Boot Fundamentals",
        description: "...",
        coverImageUrl: "..."
    }
}
```

**If serialized directly:**
```json
{
  "id": 1,
  "title": "Intro to Spring",
  "speaker": {
    "id": 5,
    "name": "John Doe",
    "email": "john@example.com",  // ❌ Exposed email
    "bio": "Very long biography...",  // ❌ Unnecessary data
    "profileImageUrl": "..."
  },
  "collection": {
    "id": 10,
    "title": "Spring Boot Fundamentals",
    "description": "Very long description...",  // ❌ Unnecessary
    "coverImageUrl": "..."
  }
}
```

**Problems:**
- ❌ Too much data (performance)
- ❌ Exposes sensitive info (email)
- ❌ Couples API to database structure

---

### The Solution: Flattening

**DTO Structure (Flattened):**
```java
LectureDto {
    id: 1,
    title: "Intro to Spring",
    speakerId: 5,           // Just the ID
    speakerName: "John Doe",  // Just the name
    collectionId: 10,
    collectionTitle: "Spring Boot Fundamentals"
}
```

**Serialized:**
```json
{
  "id": 1,
  "title": "Intro to Spring",
  "speakerId": 5,
  "speakerName": "John Doe",
  "collectionId": 10,
  "collectionTitle": "Spring Boot Fundamentals"
}
```

**Benefits:**
- ✅ Smaller payload
- ✅ Only necessary data
- ✅ No sensitive info
- ✅ Flat structure (easier for clients)

**If Client Needs Full Speaker:**
```
GET /api/v1/speakers/5
→ Returns SpeakerDto with full details
```

---

## Data Transformation in DTOs

### Example 1: Presigned URL Transformation

**Before (in database):**
```
thumbnailUrl: "thumbnails/lecture-123.jpg"
```

**After (in DTO):**
```
thumbnailUrl: "https://minio:9000/elmify-audio/thumbnails/lecture-123.jpg?X-Amz-Signature=abc123..."
```

**Code:**
```java
public static LectureDto fromEntity(Lecture lecture, StorageService storageService) {
    return new LectureDto(
        // ... other fields ...
        convertToPresignedUrl(lecture.getThumbnailUrl(), storageService),
        // ... other fields ...
    );
}
```

---

### Example 2: Date Formatting

**In Entity:**
```java
LocalDateTime createdAt = LocalDateTime.now();  // 2025-01-15T10:30:45.123
```

**In DTO (automatic by Jackson):**
```json
{
  "createdAt": "2025-01-15T10:30:45.123Z"  // ISO 8601 format
}
```

**Configured in `application.yml`:**
```yaml
spring:
  jackson:
    date-format: yyyy-MM-dd'T'HH:mm:ss.SSSXXX
    time-zone: UTC
```

---

### Example 3: Enum Transformation

**In Entity:**
```java
public enum LectureStatus {
    DRAFT, PUBLISHED, ARCHIVED
}

@Enumerated(EnumType.STRING)
private LectureStatus status;
```

**In DTO (automatic):**
```json
{
  "status": "PUBLISHED"  // String representation
}
```

---

## Validation with DTOs

### Bean Validation Annotations

**LectureDto.java:**
```java
public record LectureDto(
    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title cannot exceed 500 characters")
    String title,

    @NotNull(message = "Duration is required")
    @PositiveOrZero(message = "Duration must be non-negative")
    Integer duration,

    @NotNull(message = "File size is required")
    @PositiveOrZero(message = "File size must be positive")
    Long fileSize,

    @Email(message = "Must be a valid email")
    String contactEmail
) { }
```

### Common Validation Annotations

| Annotation | Purpose | Example |
|------------|---------|---------|
| `@NotNull` | Field cannot be null | `@NotNull String title` |
| `@NotBlank` | String cannot be null or empty | `@NotBlank String title` |
| `@Size` | String/collection size | `@Size(min=1, max=500)` |
| `@Min` / `@Max` | Number range | `@Min(0) @Max(100)` |
| `@PositiveOrZero` | Number >= 0 | `@PositiveOrZero Integer duration` |
| `@Email` | Valid email format | `@Email String email` |
| `@Pattern` | Regex pattern | `@Pattern(regexp="[A-Z]{2}\\d{4}")` |

---

### Using Validation in Controllers

**LectureController.java:**
```java
@PostMapping("/api/v1/lectures")
public ResponseEntity<LectureDto> createLecture(
    @Valid @RequestBody LectureDto lectureDto  // @Valid triggers validation
) {
    LectureDto created = lectureService.createLecture(lectureDto);
    return ResponseEntity.status(201).body(created);
}
```

**Invalid Request:**
```json
{
  "title": "",
  "duration": -100,
  "fileSize": -1
}
```

**Automatic Response (400 Bad Request):**
```json
{
  "status": 400,
  "error": "Validation failed",
  "errors": [
    {
      "field": "title",
      "rejectedValue": "",
      "message": "Title is required"
    },
    {
      "field": "duration",
      "rejectedValue": -100,
      "message": "Duration must be non-negative"
    },
    {
      "field": "fileSize",
      "rejectedValue": -1,
      "message": "File size must be positive"
    }
  ],
  "timestamp": "2025-01-15T10:30:00Z",
  "traceId": "abc-123-def"
}
```

---

## Common DTO Patterns

### 1. Input DTO vs Output DTO

Sometimes you need different DTOs for input and output:

**CreateLectureRequest.java (Input):**
```java
public record CreateLectureRequest(
    @NotBlank String title,
    @NotNull Integer duration,
    @NotNull Long speakerId,
    @NotNull Long collectionId
) { }
```

**LectureDto.java (Output):**
```java
public record LectureDto(
    Long id,  // Generated by database
    String title,
    Integer duration,
    Long speakerId,
    String speakerName,  // Included in response
    Long collectionId,
    String collectionTitle,  // Included in response
    String thumbnailUrl,  // Generated presigned URL
    LocalDateTime createdAt  // Timestamp
) { }
```

---

### 2. Pagination Wrapper

**PagedResponse.java:**
```java
public record PagedResponse<T>(
    List<T> content,         // The actual data
    int currentPage,
    int totalPages,
    long totalElements,
    int size,
    boolean hasNext,
    boolean hasPrevious
) {
    public static <T> PagedResponse<T> of(Page<T> page) {
        return new PagedResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getTotalPages(),
            page.getTotalElements(),
            page.getSize(),
            page.hasNext(),
            page.hasPrevious()
        );
    }
}
```

**Usage:**
```java
@GetMapping("/api/v1/lectures")
public PagedResponse<LectureDto> getLectures(Pageable pageable) {
    Page<LectureDto> lectures = lectureService.findAll(pageable);
    return PagedResponse.of(lectures);
}
```

**Response:**
```json
{
  "content": [
    { "id": 1, "title": "..." },
    { "id": 2, "title": "..." }
  ],
  "currentPage": 0,
  "totalPages": 10,
  "totalElements": 100,
  "size": 10,
  "hasNext": true,
  "hasPrevious": false
}
```

---

### 3. Nested DTOs (When Appropriate)

Sometimes nesting makes sense:

**FavoriteDto.java:**
```java
public record FavoriteDto(
    Long id,
    LectureDto lecture,  // Nested DTO
    LocalDateTime createdAt
) {
    public static FavoriteDto fromEntity(Favorite favorite) {
        return new FavoriteDto(
            favorite.getId(),
            LectureDto.fromEntity(favorite.getLecture()),  // Convert nested entity
            favorite.getCreatedAt()
        );
    }
}
```

**Response:**
```json
{
  "id": 1,
  "lecture": {
    "id": 86,
    "title": "Introduction to Spring",
    "duration": 3600,
    ...
  },
  "createdAt": "2025-01-15T10:30:00Z"
}
```

**When to Nest:**
- ✅ Client always needs the nested data
- ✅ Nested data is small (not hundreds of fields)
- ✅ Improves API usability

**When to Flatten:**
- ❌ Nested data is large
- ❌ Client might not need it
- ❌ Creates deep nesting (hard to work with)

---

## Best Practices

### 1. ✅ Always Use DTOs for API Responses

**Don't:**
```java
@GetMapping("/lectures/{id}")
public Lecture getLecture(@PathVariable Long id) {
    return lectureRepository.findById(id).get();  // ❌ Entity
}
```

**Do:**
```java
@GetMapping("/lectures/{id}")
public LectureDto getLecture(@PathVariable Long id) {
    Lecture lecture = lectureRepository.findById(id).get();
    return LectureDto.fromEntity(lecture);  // ✅ DTO
}
```

---

### 2. ✅ Use Static Factory Methods

**Pattern:**
```java
public record LectureDto(...) {
    public static LectureDto fromEntity(Lecture lecture) {
        // Conversion logic
    }
}
```

**Why:**
- Clear intent: "Create DTO from entity"
- Encapsulates conversion logic
- Easy to find and maintain

---

### 3. ✅ Validate Input DTOs

**Always:**
```java
@PostMapping("/lectures")
public LectureDto create(@Valid @RequestBody LectureDto dto) {
    // Validation happens automatically
}
```

---

### 4. ✅ Keep DTOs Immutable (Use Records)

**Why:**
- Thread-safe
- Easier to reason about
- Prevents accidental modification
- Better for caching

---

### 5. ✅ Don't Include Internal Fields

**Entity:**
```java
public class Lecture {
    private String fileHash;      // ❌ Internal
    private Boolean isPublic;     // ❌ Internal
    private Integer directoryId;  // ❌ Internal
}
```

**DTO:**
```java
public record LectureDto(
    // fileHash, isPublic, directoryId NOT included
    Long id,
    String title,
    Integer duration
) { }
```

---

### 6. ✅ Transform Sensitive Data

**Don't Expose:**
```json
{
  "filePath": "lectures/speaker-1/secret-lecture.mp3"  // ❌ Internal path
}
```

**Transform:**
```json
{
  "streamUrl": "https://presigned-url.com/..."  // ✅ Secure, time-limited
}
```

---

### 7. ✅ Flatten Relationships When Possible

**Don't:**
```json
{
  "speaker": {
    "id": 5,
    "name": "John Doe",
    "email": "john@example.com",
    "bio": "..."
  }
}
```

**Do:**
```json
{
  "speakerId": 5,
  "speakerName": "John Doe"
}
```

**If client needs full speaker:**
```
GET /api/v1/speakers/5
```

---

## Summary

### What We Learned

1. **DTOs = Data Transfer Objects** - Designed for transferring data between layers
2. **Entities ≠ DTOs** - Entities are for database, DTOs are for API
3. **Java Records** - Perfect for immutable DTOs
4. **fromEntity()** - Convert entity to DTO (for responses)
5. **toEntity()** - Convert DTO to entity (for requests)
6. **Flattening** - Extract only needed fields from relationships
7. **Transformation** - Convert internal data to client-friendly format
8. **Validation** - Ensure data integrity with Bean Validation

### The DTO Pattern Flow

```
1. Client sends request
    ↓
2. Controller receives DTO (@Valid triggers validation)
    ↓
3. Service converts DTO to Entity (toEntity())
    ↓
4. Service saves Entity to database
    ↓
5. Service retrieves Entity from database
    ↓
6. Service converts Entity to DTO (fromEntity())
    ↓
7. Controller returns DTO to client
    ↓
8. Jackson serializes DTO to JSON
    ↓
9. Client receives JSON response
```

### Key Benefits

1. ✅ **Security**: Don't expose internal data
2. ✅ **Performance**: Smaller payloads
3. ✅ **Stability**: API independent of database schema
4. ✅ **Flexibility**: Transform data as needed
5. ✅ **Validation**: Built-in data integrity
6. ✅ **Clarity**: Clear API contracts

---

**Next Steps:**
- Read the **Dependency Injection & Spring Beans** guide to understand how DTOs, Services, and Repositories work together
- Experiment with creating your own DTO
- Try adding validation to existing DTOs

---

**Related Topics:**
- [Understanding JPA and Hibernate](./understanding-jpa-and-hibernate.md)
- [Understanding @ManyToOne Relationships](./understanding-manytoone-relationships.md)
- [Understanding RESTful API Design Patterns](./understanding-restful-api-design.md)
