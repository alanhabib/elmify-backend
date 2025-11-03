# Understanding Spring Data JPA Query Methods

## Table of Contents
1. [What are JPA Query Methods?](#what-are-jpa-query-methods)
2. [The Magic of Method Names](#the-magic-of-method-names)
3. [Query Derivation from Method Names](#query-derivation-from-method-names)
4. [Your Application's Query Methods](#your-applications-query-methods)
5. [@Query Annotation (Custom JPQL)](#query-annotation-custom-jpql)
6. [Pagination and Sorting](#pagination-and-sorting)
7. [JOIN FETCH for Performance](#join-fetch-for-performance)
8. [@Modifying for Updates](#modifying-for-updates)
9. [Query Method Return Types](#query-method-return-types)
10. [Best Practices](#best-practices)

---

## What are JPA Query Methods?

**JPA Query Methods** let you define database queries by simply declaring method signatures in your repository interface - **no SQL code required** (usually).

### The Traditional Way (Before Spring Data JPA)

```java
public class LectureRepository {
    public List<Lecture> findByCollectionId(Long collectionId) {
        String sql = "SELECT * FROM lectures WHERE collection_id = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setLong(1, collectionId);
        ResultSet rs = stmt.executeQuery();

        List<Lecture> lectures = new ArrayList<>();
        while (rs.next()) {
            Lecture lecture = new Lecture();
            lecture.setId(rs.getLong("id"));
            lecture.setTitle(rs.getString("title"));
            // ... 20 more lines of mapping ...
            lectures.add(lecture);
        }
        return lectures;
    }
}
```

**Problems:**
- ❌ Lots of boilerplate code
- ❌ Error-prone (typos in column names)
- ❌ Manual mapping from ResultSet to objects
- ❌ Must write SQL for every query

---

### The Spring Data JPA Way

```java
public interface LectureRepository extends JpaRepository<Lecture, Long> {

    // Spring generates the implementation automatically!
    Page<Lecture> findByCollectionId(Long collectionId, Pageable pageable);

}
```

**Benefits:**
- ✅ **Zero SQL code** - Spring derives query from method name
- ✅ **Type-safe** - Compiler catches errors
- ✅ **Less code** - One line instead of 30
- ✅ **Automatic mapping** - ResultSet → Lecture done for you

---

### How It Works

```
┌──────────────────────────────────────────────────────────────┐
│ 1. You Define Interface Method                               │
│    Page<Lecture> findByCollectionId(Long id, Pageable p);   │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 2. Spring Analyzes Method Name at Startup                    │
│    - "findBy" → SELECT query                                 │
│    - "CollectionId" → WHERE collection_id = ?               │
│    - Returns Page → Add pagination                           │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 3. Spring Generates SQL                                      │
│    SELECT l FROM Lecture l WHERE l.collection.id = :id      │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 4. Spring Creates Proxy Implementation                       │
│    (You never see this code - it's generated at runtime)     │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 5. You Call the Method                                       │
│    Page<Lecture> lectures = repository.findByCollectionId(5, pageable); │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 6. SQL Executed                                              │
│    SELECT * FROM lectures WHERE collection_id = 5           │
│    ORDER BY id ASC LIMIT 20 OFFSET 0                        │
└──────────────────────────────────────────────────────────────┘
```

---

## The Magic of Method Names

Spring Data JPA parses method names to derive queries. The naming follows a strict pattern:

### Method Name Structure

```
[action][Subject]By[Predicate][OrderBy][Property][Direction]
```

**Examples:**

| Method Name | Generated Query |
|-------------|----------------|
| `findByTitle(String title)` | `WHERE title = ?` |
| `findByTitleContaining(String keyword)` | `WHERE title LIKE '%keyword%'` |
| `findBySpeakerId(Long id)` | `WHERE speaker_id = ?` |
| `findByTitleAndSpeakerId(String title, Long id)` | `WHERE title = ? AND speaker_id = ?` |
| `findByTitleOrDescription(String search)` | `WHERE title = ? OR description = ?` |
| `findByPlayCountGreaterThan(Integer count)` | `WHERE play_count > ?` |
| `findByCreatedAtBefore(LocalDateTime date)` | `WHERE created_at < ?` |
| `findByTitleOrderByCreatedAtDesc(String title)` | `WHERE title = ? ORDER BY created_at DESC` |

---

### Supported Keywords

#### **Action Keywords**

| Keyword | Purpose | Example |
|---------|---------|---------|
| `find...By` | Read query | `findByTitle()` |
| `read...By` | Same as find | `readByTitle()` |
| `get...By` | Same as find | `getByTitle()` |
| `query...By` | Same as find | `queryByTitle()` |
| `count...By` | Count rows | `countBySpeakerId()` |
| `exists...By` | Check if exists | `existsByTitle()` |
| `delete...By` | Delete query | `deleteByTitle()` |

**Most common:** `find...By`

---

#### **Predicate Keywords**

| Keyword | SQL Equivalent | Example |
|---------|----------------|---------|
| `Is` / `Equals` | `= ?` | `findByTitleIs(String title)` |
| `Not` | `!= ?` | `findByTitleNot(String title)` |
| `Containing` | `LIKE '%?%'` | `findByTitleContaining(String keyword)` |
| `StartingWith` | `LIKE '?%'` | `findByTitleStartingWith(String prefix)` |
| `EndingWith` | `LIKE '%?'` | `findByTitleEndingWith(String suffix)` |
| `GreaterThan` | `> ?` | `findByPlayCountGreaterThan(Integer count)` |
| `LessThan` | `< ?` | `findByDurationLessThan(Integer seconds)` |
| `Between` | `BETWEEN ? AND ?` | `findByDurationBetween(Integer min, Integer max)` |
| `Before` | `< ?` | `findByCreatedAtBefore(LocalDateTime date)` |
| `After` | `> ?` | `findByCreatedAtAfter(LocalDateTime date)` |
| `IsNull` | `IS NULL` | `findByDescriptionIsNull()` |
| `IsNotNull` | `IS NOT NULL` | `findByDescriptionIsNotNull()` |
| `In` | `IN (?)` | `findByIdIn(List<Long> ids)` |
| `NotIn` | `NOT IN (?)` | `findByIdNotIn(List<Long> ids)` |

---

#### **Logical Operators**

| Keyword | SQL Equivalent | Example |
|---------|----------------|---------|
| `And` | `AND` | `findByTitleAndSpeakerId(String t, Long s)` |
| `Or` | `OR` | `findByTitleOrDescription(String search)` |

---

#### **Sorting Keywords**

| Keyword | SQL Equivalent | Example |
|---------|----------------|---------|
| `OrderBy[Property]Asc` | `ORDER BY property ASC` | `findByTitleOrderByCreatedAtAsc()` |
| `OrderBy[Property]Desc` | `ORDER BY property DESC` | `findByTitleOrderByCreatedAtDesc()` |

---

## Query Derivation from Method Names

### Example 1: Simple Equality

```java
// Method
Page<Lecture> findByCollectionId(Long collectionId, Pageable pageable);

// Generated JPQL
SELECT l FROM Lecture l WHERE l.collection.id = :collectionId

// Generated SQL (PostgreSQL)
SELECT * FROM lectures WHERE collection_id = ? ORDER BY id ASC LIMIT 20 OFFSET 0
```

---

### Example 2: Nested Property Access

```java
// Method
Page<Lecture> findBySpeakerId(Long speakerId, Pageable pageable);

// Entity relationship
@Entity
public class Lecture {
    @ManyToOne
    private Speaker speaker;  // ← Nested object
}

// Generated JPQL
SELECT l FROM Lecture l WHERE l.speaker.id = :speakerId

// Generated SQL
SELECT * FROM lectures WHERE speaker_id = ?
```

**Key Point:** `SpeakerId` in the method name means "the ID of the nested speaker object".

---

### Example 3: Like Query (Search)

```java
// Method (hypothetical)
Page<Lecture> findByTitleContaining(String keyword, Pageable pageable);

// Generated JPQL
SELECT l FROM Lecture l WHERE LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%'))

// Generated SQL
SELECT * FROM lectures WHERE LOWER(title) LIKE LOWER('%' || ? || '%')

// Example call
repository.findByTitleContaining("philosophy", pageable);
// Matches: "Introduction to Philosophy", "Philosophy 101", "Ancient Philosophy"
```

---

### Example 4: Multiple Conditions (AND)

```java
// Method (hypothetical)
List<Lecture> findBySpeakerIdAndDurationGreaterThan(Long speakerId, Integer minDuration);

// Generated JPQL
SELECT l FROM Lecture l WHERE l.speaker.id = :speakerId AND l.duration > :minDuration

// Generated SQL
SELECT * FROM lectures WHERE speaker_id = ? AND duration > ?

// Example call
repository.findBySpeakerIdAndDurationGreaterThan(5L, 3600);
// Returns: Lectures by speaker 5 longer than 1 hour
```

---

### Example 5: OR Condition

```java
// Method (hypothetical)
List<Lecture> findByTitleContainingOrDescriptionContaining(String keyword);

// Generated JPQL
SELECT l FROM Lecture l
WHERE LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
   OR LOWER(l.description) LIKE LOWER(CONCAT('%', :keyword, '%'))

// Example call
repository.findByTitleContainingOrDescriptionContaining("plato");
// Matches: Lectures with "plato" in title OR description
```

---

## Your Application's Query Methods

### LectureRepository

```java
@Repository
public interface LectureRepository extends JpaRepository<Lecture, Long> {

    // 1. Find by collection (derived query)
    Page<Lecture> findByCollectionId(Long collectionId, Pageable pageable);
    // SQL: WHERE collection_id = ?

    // 2. Find by speaker (derived query)
    Page<Lecture> findBySpeakerId(Long speakerId, Pageable pageable);
    // SQL: WHERE speaker_id = ?

    // 3. Custom JPQL with JOIN FETCH (manual query)
    @Query("SELECT l FROM Lecture l LEFT JOIN FETCH l.speaker LEFT JOIN FETCH l.collection")
    Page<Lecture> findAllWithSpeakerAndCollection(Pageable pageable);

    // 4. Custom search query
    @Query("SELECT l FROM Lecture l WHERE " +
           "LOWER(l.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.speaker.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.collection.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Lecture> searchByTitleOrSpeakerOrCollectionName(
        @Param("searchTerm") String searchTerm, Pageable pageable);

    // 5. Update query (modifying)
    @Modifying
    @Transactional
    @Query("UPDATE Lecture l SET l.playCount = l.playCount + 1, l.lastPlayedAt = :timestamp WHERE l.id = :lectureId")
    void incrementPlayCount(@Param("lectureId") Long lectureId, @Param("timestamp") LocalDateTime timestamp);
}
```

---

### FavoriteRepository

```java
@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    // 1. Find favorites with eager loading (custom JPQL)
    @Query("SELECT f FROM Favorite f LEFT JOIN FETCH f.lecture l LEFT JOIN FETCH l.speaker LEFT JOIN FETCH l.collection WHERE f.user = :user")
    Page<Favorite> findByUserWithLecture(@Param("user") User user, Pageable pageable);

    // 2. Check if favorite exists (derived query)
    boolean existsByUserAndLectureId(User user, Long lectureId);
    // SQL: SELECT CASE WHEN COUNT(f) > 0 THEN TRUE ELSE FALSE END
    //      FROM favorites f WHERE f.user_id = ? AND f.lecture_id = ?

    // 3. Find specific favorite (derived query)
    Optional<Favorite> findByUserAndLectureId(User user, Long lectureId);
    // SQL: SELECT * FROM favorites WHERE user_id = ? AND lecture_id = ?

    // 4. Delete favorite (derived query)
    void deleteByUserAndLectureId(User user, Long lectureId);
    // SQL: DELETE FROM favorites WHERE user_id = ? AND lecture_id = ?

    // 5. Count user's favorites (derived query)
    long countByUser(User user);
    // SQL: SELECT COUNT(*) FROM favorites WHERE user_id = ?
}
```

---

### UserRepository

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find by Clerk ID (derived query)
    Optional<User> findByClerkId(String clerkId);
    // SQL: SELECT * FROM users WHERE clerk_id = ?

    // Find by email (derived query)
    Optional<User> findByEmail(String email);
    // SQL: SELECT * FROM users WHERE email = ?

    // Check if email exists (derived query)
    boolean existsByEmail(String email);
    // SQL: SELECT CASE WHEN COUNT(u) > 0 THEN TRUE ELSE FALSE END
    //      FROM users u WHERE email = ?
}
```

---

## @Query Annotation (Custom JPQL)

When method name derivation isn't enough, use `@Query` to write custom JPQL (Java Persistence Query Language).

### JPQL vs SQL

**JPQL** is like SQL, but operates on **entities** (Java objects) instead of tables.

```java
// JPQL (operates on entities)
@Query("SELECT l FROM Lecture l WHERE l.speaker.name = :speakerName")
List<Lecture> findBySpeakerName(@Param("speakerName") String speakerName);

// Equivalent SQL
SELECT * FROM lectures l
JOIN speakers s ON l.speaker_id = s.id
WHERE s.name = ?
```

**Key Differences:**

| JPQL | SQL |
|------|-----|
| `SELECT l FROM Lecture l` | `SELECT * FROM lectures l` |
| `l.speaker.name` | `JOIN speakers ... s.name` |
| `l.collection.title` | `JOIN collections ... c.title` |
| Entity names (Lecture) | Table names (lectures) |
| Property names (speakerId) | Column names (speaker_id) |

JPQL automatically handles joins when you reference nested properties.

---

### Example 1: Search Across Multiple Fields

```java
@Query("SELECT l FROM Lecture l WHERE " +
       "LOWER(l.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
       "LOWER(l.speaker.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
       "LOWER(l.collection.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
Page<Lecture> searchByTitleOrSpeakerOrCollectionName(
    @Param("searchTerm") String searchTerm, Pageable pageable);
```

**Why Custom Query?**
- Method name would be too long: `findByTitleContainingOrSpeakerNameContainingOrCollectionTitleContaining`
- Need case-insensitive search (`LOWER()`)

**Generated SQL:**
```sql
SELECT l.* FROM lectures l
LEFT JOIN speakers s ON l.speaker_id = s.id
LEFT JOIN collections c ON l.collection_id = c.id
WHERE LOWER(l.title) LIKE LOWER('%' || ? || '%')
   OR LOWER(s.name) LIKE LOWER('%' || ? || '%')
   OR LOWER(c.title) LIKE LOWER('%' || ? || '%')
ORDER BY l.id ASC
LIMIT 20 OFFSET 0
```

---

### Example 2: Eager Loading with JOIN FETCH

```java
@Query("SELECT l FROM Lecture l LEFT JOIN FETCH l.speaker LEFT JOIN FETCH l.collection")
Page<Lecture> findAllWithSpeakerAndCollection(Pageable pageable);
```

**Purpose:** Load `speaker` and `collection` in a **single query** to avoid lazy loading issues.

**Without JOIN FETCH (N+1 Problem):**
```sql
-- 1 query to get lectures
SELECT * FROM lectures LIMIT 20;

-- 20 queries to get each lecture's speaker (N+1 problem!)
SELECT * FROM speakers WHERE id = 1;
SELECT * FROM speakers WHERE id = 2;
...
SELECT * FROM speakers WHERE id = 20;

-- 20 more queries to get each lecture's collection
SELECT * FROM collections WHERE id = 1;
SELECT * FROM collections WHERE id = 2;
...
```

**With JOIN FETCH (1 Query):**
```sql
SELECT l.*, s.*, c.*
FROM lectures l
LEFT JOIN speakers s ON l.speaker_id = s.id
LEFT JOIN collections c ON l.collection_id = c.id
LIMIT 20;
```

**Result:** 1 query instead of 41 queries!

---

### Example 3: Named Parameters

```java
@Query("SELECT f FROM Favorite f WHERE f.user.clerkId = :clerkId AND f.lecture.id = :lectureId")
Optional<Favorite> findByClerkIdAndLectureId(
    @Param("clerkId") String clerkId,
    @Param("lectureId") Long lectureId
);
```

**Named parameters** (`:clerkId`, `:lectureId`) are clearer than positional parameters (`?1`, `?2`).

---

## Pagination and Sorting

### Pageable Interface

Spring Data provides `Pageable` for pagination and sorting:

```java
Page<Lecture> findByCollectionId(Long collectionId, Pageable pageable);
```

**How to Use:**

```java
// Service Layer
public Page<Lecture> getLecturesByCollection(Long collectionId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("title").ascending());
    return lectureRepository.findByCollectionId(collectionId, pageable);
}

// Controller Layer
@GetMapping("/collection/{id}")
public ResponseEntity<PagedResponse<LectureDto>> getLectures(
    @PathVariable Long id,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size) {

    Pageable pageable = PageRequest.of(page, size);
    Page<Lecture> lectures = lectureService.getLecturesByCollection(id, pageable);
    return ResponseEntity.ok(PagedResponse.from(lectures.map(l -> LectureDto.fromEntity(l, storageService))));
}
```

**Request:**
```http
GET /api/v1/lectures/collection/5?page=0&size=20&sort=title,asc
```

**Spring Automatically Parses:**
- `page=0` → First page
- `size=20` → 20 items per page
- `sort=title,asc` → Sort by title ascending

---

### Sorting Options

```java
// Single field, ascending
Pageable pageable = PageRequest.of(0, 20, Sort.by("title").ascending());

// Single field, descending
Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());

// Multiple fields
Pageable pageable = PageRequest.of(0, 20,
    Sort.by("speaker.name").ascending()
        .and(Sort.by("title").ascending())
);

// Unsorted
Pageable pageable = PageRequest.of(0, 20);
```

---

### Page vs List

| Return Type | Use Case | Pagination Support |
|-------------|----------|--------------------|
| `Page<Lecture>` | API endpoints (need total count) | Yes, includes total pages/elements |
| `List<Lecture>` | Internal processing (don't need count) | No, just returns results |
| `Slice<Lecture>` | "Load more" UI (don't need total) | Partial, knows if there's a next page |

**Page Example:**
```java
Page<Lecture> page = repository.findAll(pageable);
page.getContent();        // List<Lecture> for current page
page.getTotalElements();  // Total count across all pages
page.getTotalPages();     // Total pages
page.getNumber();         // Current page number
page.isFirst();           // Is this the first page?
page.isLast();            // Is this the last page?
```

---

## JOIN FETCH for Performance

### The N+1 Query Problem

**Problem Code:**
```java
// Controller
@GetMapping
public List<LectureDto> getAllLectures() {
    List<Lecture> lectures = lectureRepository.findAll();  // 1 query
    return lectures.stream()
        .map(lecture -> LectureDto.fromEntity(lecture, storageService))  // N queries!
        .toList();
}

// LectureDto.fromEntity()
public static LectureDto fromEntity(Lecture lecture, StorageService storage) {
    return new LectureDto(
        lecture.getId(),
        lecture.getTitle(),
        lecture.getSpeaker().getName(),  // ← Triggers query for speaker!
        lecture.getCollection().getTitle()  // ← Triggers query for collection!
    );
}
```

**Generated Queries:**
```sql
-- 1. Initial query
SELECT * FROM lectures;

-- 2-21. One query PER lecture to get speaker (N+1 problem)
SELECT * FROM speakers WHERE id = 1;
SELECT * FROM speakers WHERE id = 2;
...
SELECT * FROM speakers WHERE id = 20;

-- 22-41. One query PER lecture to get collection
SELECT * FROM collections WHERE id = 1;
SELECT * FROM collections WHERE id = 2;
...
SELECT * FROM collections WHERE id = 20;
```

**Total: 41 queries for 20 lectures!** 🐢

---

### Solution: JOIN FETCH

```java
@Query("SELECT l FROM Lecture l LEFT JOIN FETCH l.speaker LEFT JOIN FETCH l.collection")
Page<Lecture> findAllWithSpeakerAndCollection(Pageable pageable);
```

**Generated Query:**
```sql
SELECT l.*, s.*, c.*
FROM lectures l
LEFT JOIN speakers s ON l.speaker_id = s.id
LEFT JOIN collections c ON l.collection_id = c.id
LIMIT 20;
```

**Total: 1 query for 20 lectures!** 🚀

---

### Your Usage

```java
// LectureService.java
public Page<Lecture> getAllLectures(Pageable pageable) {
    return lectureRepository.findAllWithSpeakerAndCollection(pageable);
    // Returns Lecture objects with speaker and collection already loaded
}

// Controller
@GetMapping
public ResponseEntity<PagedResponse<LectureDto>> getAllLectures(Pageable pageable) {
    Page<LectureDto> lectureDtos = lectureService.getAllLectures(pageable)
        .map(lecture -> LectureDto.fromEntity(lecture, storageService));
    // No additional queries when accessing lecture.getSpeaker() or lecture.getCollection()
    return ResponseEntity.ok(PagedResponse.from(lectureDtos));
}
```

---

## @Modifying for Updates

Use `@Modifying` for UPDATE or DELETE queries.

### Example: Increment Play Count

```java
@Modifying
@Transactional
@Query("UPDATE Lecture l SET l.playCount = l.playCount + 1, l.lastPlayedAt = :timestamp WHERE l.id = :lectureId")
void incrementPlayCount(@Param("lectureId") Long lectureId, @Param("timestamp") LocalDateTime timestamp);
```

**Why @Modifying?**
- Tells Spring this is a write operation (not a read)
- Automatically clears the persistence context after execution

**Why @Transactional?**
- Ensures the update happens in a transaction
- Rolls back if there's an error

**Generated SQL:**
```sql
UPDATE lectures
SET play_count = play_count + 1,
    last_played_at = ?
WHERE id = ?
```

---

### Example: Bulk Delete

```java
@Modifying
@Transactional
@Query("DELETE FROM Favorite f WHERE f.user.id = :userId AND f.lecture.id = :lectureId")
void deleteFavorite(@Param("userId") Long userId, @Param("lectureId") Long lectureId);
```

**Or use derived delete:**
```java
void deleteByUserAndLectureId(User user, Long lectureId);
```

Both work, but `@Query` gives you more control.

---

## Query Method Return Types

Spring Data JPA supports many return types:

| Return Type | Use Case | Example |
|-------------|----------|---------|
| `Lecture` | Single result (throws if >1) | `findByClerkId(String id)` |
| `Optional<Lecture>` | Single result, may not exist | `findById(Long id)` |
| `List<Lecture>` | Multiple results | `findBySpeakerId(Long id)` |
| `Page<Lecture>` | Paginated with total count | `findAll(Pageable p)` |
| `Slice<Lecture>` | Paginated without total count | `findAll(Pageable p)` |
| `Stream<Lecture>` | Lazy stream (for large datasets) | `streamBySpeakerId(Long id)` |
| `boolean` | Check existence | `existsByEmail(String email)` |
| `long` | Count results | `countBySpeakerId(Long id)` |
| `void` | Modifying query | `deleteByLectureId(Long id)` |

---

### Optional for Single Results

```java
Optional<User> findByClerkId(String clerkId);

// Usage
Optional<User> userOpt = userRepository.findByClerkId("user_123");
User user = userOpt.orElseThrow(() -> new ResourceNotFoundException("User not found"));
```

**Why Optional?**
- Makes it explicit that the result may not exist
- Prevents `NullPointerException`

---

### boolean for Existence Checks

```java
boolean existsByUserAndLectureId(User user, Long lectureId);

// Usage
if (favoriteRepository.existsByUserAndLectureId(user, lectureId)) {
    return Map.of("isFavorited", true);
}
```

**Efficient:** Generates `SELECT COUNT(*)` instead of fetching all data.

---

### long for Counting

```java
long countByUser(User user);

// Usage
long favoriteCount = favoriteRepository.countByUser(user);
```

**Generated SQL:**
```sql
SELECT COUNT(*) FROM favorites WHERE user_id = ?
```

---

## Best Practices

### 1. Use Method Name Derivation for Simple Queries

✅ **GOOD:**
```java
Page<Lecture> findByCollectionId(Long collectionId, Pageable pageable);
Optional<User> findByClerkId(String clerkId);
boolean existsByEmail(String email);
```

❌ **BAD (unnecessary @Query):**
```java
@Query("SELECT l FROM Lecture l WHERE l.collection.id = :collectionId")
Page<Lecture> findByCollectionId(@Param("collectionId") Long collectionId, Pageable pageable);
```

**Rule:** If Spring can derive the query from the method name, let it.

---

### 2. Use @Query for Complex Queries

✅ **GOOD:**
```java
@Query("SELECT l FROM Lecture l WHERE " +
       "LOWER(l.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
       "LOWER(l.speaker.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
Page<Lecture> searchLectures(@Param("searchTerm") String searchTerm, Pageable pageable);
```

**Rule:** Use `@Query` when:
- Method name would be too long or unclear
- You need custom SQL logic (LOWER, CONCAT, etc.)
- You need JOIN FETCH for performance

---

### 3. Always Use JOIN FETCH for Entities with Relationships

❌ **BAD (N+1 queries):**
```java
List<Lecture> findAll();  // Lazy loads speaker and collection
```

✅ **GOOD:**
```java
@Query("SELECT l FROM Lecture l LEFT JOIN FETCH l.speaker LEFT JOIN FETCH l.collection")
Page<Lecture> findAllWithSpeakerAndCollection(Pageable pageable);
```

**Rule:** If your DTO accesses nested objects (`lecture.getSpeaker().getName()`), use JOIN FETCH.

---

### 4. Use Pagination for Large Result Sets

❌ **BAD:**
```java
List<Lecture> findAll();  // Returns 10,000 lectures!
```

✅ **GOOD:**
```java
Page<Lecture> findAll(Pageable pageable);  // Returns 20 lectures per page
```

**Rule:** APIs should **always** return paginated results.

---

### 5. Use Named Parameters in @Query

❌ **BAD (positional parameters):**
```java
@Query("SELECT l FROM Lecture l WHERE l.speaker.id = ?1 AND l.duration > ?2")
List<Lecture> findBySpeakerAndDuration(Long speakerId, Integer duration);
```

✅ **GOOD (named parameters):**
```java
@Query("SELECT l FROM Lecture l WHERE l.speaker.id = :speakerId AND l.duration > :duration")
List<Lecture> findBySpeakerAndDuration(@Param("speakerId") Long speakerId, @Param("duration") Integer duration);
```

**Rule:** Named parameters are clearer and less error-prone.

---

### 6. Use Optional for Single Results That May Not Exist

❌ **BAD (can return null):**
```java
User findByClerkId(String clerkId);  // Returns null if not found
```

✅ **GOOD:**
```java
Optional<User> findByClerkId(String clerkId);

// Usage
User user = userRepository.findByClerkId("user_123")
    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
```

**Rule:** Use `Optional<T>` for single results, `List<T>` for multiple.

---

### 7. Use @Transactional with @Modifying

❌ **BAD (may fail silently):**
```java
@Modifying
@Query("UPDATE Lecture l SET l.playCount = l.playCount + 1 WHERE l.id = :id")
void incrementPlayCount(@Param("id") Long id);
```

✅ **GOOD:**
```java
@Modifying
@Transactional
@Query("UPDATE Lecture l SET l.playCount = l.playCount + 1 WHERE l.id = :id")
void incrementPlayCount(@Param("id") Long id);
```

**Rule:** Always pair `@Modifying` with `@Transactional`.

---

### 8. Avoid Fetching Entire Objects for Existence Checks

❌ **BAD:**
```java
Optional<User> findByEmail(String email);

// Usage
if (userRepository.findByEmail("test@example.com").isPresent()) {
    // Fetched entire user object just to check existence!
}
```

✅ **GOOD:**
```java
boolean existsByEmail(String email);

// Usage
if (userRepository.existsByEmail("test@example.com")) {
    // Only runs COUNT query
}
```

**Rule:** Use `existsBy...` for existence checks, not `findBy...`.

---

## Key Takeaways

### What You Learned

1. **Query Methods Eliminate Boilerplate**
   - Spring derives queries from method names
   - No SQL code needed for simple queries
   - Type-safe and compiler-checked

2. **Method Naming Convention**
   - `findBy[Property]` → WHERE property = ?
   - `findBy[Property]Containing` → WHERE property LIKE '%?%'
   - `findBy[Property]And[Property2]` → WHERE property = ? AND property2 = ?
   - `OrderBy[Property]Desc` → ORDER BY property DESC

3. **@Query for Complex Queries**
   - Use when method name is too long
   - Write JPQL (operates on entities, not tables)
   - Named parameters (`:paramName`) are clearer

4. **JOIN FETCH Prevents N+1 Queries**
   - Loads related entities in one query
   - Critical for performance
   - Use when DTOs access nested objects

5. **Pagination is Essential**
   - Always return `Page<T>` for APIs
   - Includes total count, current page, etc.
   - Spring auto-parses `?page=0&size=20&sort=title,asc`

6. **Return Type Matters**
   - `Optional<T>` → Single result that may not exist
   - `List<T>` → Multiple results
   - `Page<T>` → Paginated results with total count
   - `boolean` → Existence check
   - `long` → Count query

### How This Connects to Your Application

```
Controller (LectureController)
      ↓ Calls
Service Layer (LectureService)
      ↓ Calls
Repository (LectureRepository)
      ↓ Query Methods
    findByCollectionId(Long id, Pageable p)
    findAllWithSpeakerAndCollection(Pageable p)
      ↓ Spring Generates
    JPQL → SQL
      ↓ Executes
PostgreSQL Database
      ↓ Returns
    ResultSet → Lecture entities
      ↓ Mapped to
    LectureDto (via fromEntity)
      ↓ Returned to
React Native App
```

### Next Steps

- [Understanding JPA and Hibernate](./understanding-jpa-and-hibernate.md) - How entities work
- [Understanding @ManyToOne Relationships](./understanding-manytoone-relationships.md) - How relationships are queried
- [Understanding RESTful API Design](./understanding-restful-api-design.md) - How pagination works in APIs

---

**Congratulations!** You now understand how Spring Data JPA query methods work, from simple derived queries to complex custom JPQL with JOIN FETCH for optimal performance.
