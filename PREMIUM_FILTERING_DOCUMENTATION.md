# Premium Content Filtering - Implementation Documentation

## Overview

This document describes the comprehensive premium content filtering system implemented across the Elmify backend. The system ensures that non-premium users cannot access premium content (speakers, collections, and lectures).

## Architecture

### Core Principle: Cascading Premium Status

**Premium status cascades from Speaker → Collection → Lecture**

- If a Speaker is marked as `isPremium = true`, all their collections and lectures are automatically premium
- Premium filtering is applied at the service layer before data is returned to clients
- Centralized filtering logic ensures consistent behavior across all endpoints

### Best Practices Applied

1. **Single Responsibility Principle**: All premium filtering logic is centralized in `PremiumFilterService`
2. **DRY (Don't Repeat Yourself)**: Reusable filter methods instead of duplicating logic
3. **Separation of Concerns**: Business logic in services, authentication via Spring Security
4. **Defensive Programming**: Filters applied on all list endpoints to prevent data leakage

---

## Implementation Details

### 1. PremiumFilterService (Centralized Filtering Logic)

**Location**: `src/main/java/com/elmify/backend/service/PremiumFilterService.java`

**Purpose**: Provides reusable methods for filtering premium content and checking user access.

**Key Methods**:

```java
// Check if current user has premium access
public boolean isCurrentUserPremium()

// Filter Page<Lecture> - removes premium lectures for non-premium users
public Page<Lecture> filterLectures(Page<Lecture> lectures, Pageable pageable)

// Filter List<Lecture> - removes premium lectures for non-premium users
public List<Lecture> filterLecturesList(List<Lecture> lectures)

// Filter Page<Collection> - removes premium collections for non-premium users
public Page<Collection> filterCollections(Page<Collection> collections, Pageable pageable)

// Filter List<Collection> - removes premium collections for non-premium users
public List<Collection> filterCollectionsList(List<Collection> collections)

// Check if user can access a specific lecture
public boolean canAccessLecture(Lecture lecture)

// Check if user can access a specific collection
public boolean canAccessCollection(Collection collection)
```

**Authentication Flow**:
1. Gets `Authentication` from `SecurityContextHolder`
2. Extracts Clerk ID from authenticated user
3. Looks up user in database
4. Returns user's `isPremium` status

---

### 2. Entity-Level Premium Status

**Speaker.java** (`src/main/java/com/elmify/backend/entity/Speaker.java`):
```java
@Column(name = "is_premium")
private Boolean isPremium;
```

**Collection.java** (`src/main/java/com/elmify/backend/entity/Collection.java`):
```java
public boolean isPremium() {
    return speaker != null && speaker.getIsPremium() != null && speaker.getIsPremium();
}
```

**Lecture.java** (`src/main/java/com/elmify/backend/entity/Lecture.java`):
```java
public boolean isPremium() {
    return speaker != null && speaker.getIsPremium() != null && speaker.getIsPremium();
}
```

**Important**: Collections and Lectures inherit premium status from their Speaker, not from a database column.

---

### 3. Service Layer Filtering

#### SpeakerService

**Location**: `src/main/java/com/elmify/backend/service/SpeakerService.java`

**Filtered Endpoints**:
- `getAllSpeakers(Pageable pageable)` - Filters out premium speakers for non-premium users

**Implementation**:
```java
public Page<SpeakerDto> getAllSpeakers(Pageable pageable) {
    Page<Speaker> speakers = speakerRepository.findAll(pageable);

    boolean userIsPremium = isCurrentUserPremium();

    if (!userIsPremium) {
        List<Speaker> filteredSpeakers = speakers.getContent().stream()
                .filter(speaker -> speaker.getIsPremium() == null || !speaker.getIsPremium())
                .collect(Collectors.toList());

        speakers = new PageImpl<>(filteredSpeakers, pageable, filteredSpeakers.size());
    }

    return speakers.map(speaker -> SpeakerDto.fromEntity(speaker, storageService));
}
```

---

#### LectureService

**Location**: `src/main/java/com/elmify/backend/service/LectureService.java`

**Filtered Endpoints**:
- `getAllLectures(Pageable pageable)` - All lectures, filtered by premium status
- `getTrendingLectures(Pageable pageable)` - **CRITICAL**: Most Popular section filtered
- `getLecturesByCollectionId(Long collectionId, Pageable pageable)` - Collection's lectures filtered
- `getLecturesBySpeakerId(Long speakerId, Pageable pageable)` - Speaker's lectures filtered

**Implementation**:
```java
public Page<Lecture> getTrendingLectures(Pageable pageable) {
    Page<Lecture> lectures = lectureRepository.findAllOrderByPlayCountDesc(pageable);
    return premiumFilterService.filterLectures(lectures, pageable);
}
```

---

#### CollectionService

**Location**: `src/main/java/com/elmify/backend/service/CollectionService.java`

**Filtered Endpoints**:
- `getAllCollections(Pageable pageable)` - All collections, filtered by premium status
- `getCollectionsBySpeakerId(Long speakerId, Pageable pageable)` - Speaker's collections filtered

**Implementation**:
```java
public Page<Collection> getAllCollections(Pageable pageable) {
    Page<Collection> collections = collectionRepository.findAll(pageable);
    return premiumFilterService.filterCollections(collections, pageable);
}
```

---

#### FavoriteService

**Location**: `src/main/java/com/elmify/backend/service/FavoriteService.java`

**Filtered Endpoints**:
- `getUserFavorites(String clerkId, Pageable pageable)` - User's favorites, premium lectures filtered out

**Additional Protection**:
- `addFavorite(String clerkId, Long lectureId)` - Prevents non-premium users from favoriting premium lectures

**Implementation**:
```java
public Favorite addFavorite(String clerkId, Long lectureId) {
    // ... user and lecture lookup ...

    // Check if user can access this lecture (premium check)
    if (!premiumFilterService.canAccessLecture(lecture)) {
        throw new RuntimeException("Cannot favorite premium content without premium access");
    }

    // Create and save favorite
    Favorite favorite = new Favorite(user, lecture);
    return favoriteRepository.save(favorite);
}
```

---

## Security Considerations

### What is Protected:

✅ **Speakers List** - Non-premium users cannot see premium speakers
✅ **Collections List** - Premium speakers' collections are hidden
✅ **Lectures List** - Premium speakers' lectures are hidden
✅ **Trending/Most Popular** - Premium lectures filtered out
✅ **Speaker's Collections** - If speaker is premium, collections are hidden
✅ **Collection's Lectures** - If speaker is premium, lectures are hidden
✅ **User Favorites** - Premium lectures removed from favorites list
✅ **Add to Favorites** - Cannot favorite premium content without access

### Important Notes:

1. **Frontend Responsibilities**:
   - Frontend still needs to handle "premium" badges/icons
   - Frontend should not hardcode assumptions about what's premium
   - Always rely on API response data

2. **Database vs. Runtime**:
   - Only `Speaker.isPremium` is stored in database
   - `Collection.isPremium()` and `Lecture.isPremium()` are computed at runtime
   - This ensures consistency: if speaker becomes premium, all content becomes premium

3. **Performance Considerations**:
   - Filtering happens in-memory after database fetch
   - For large datasets, consider database-level filtering with JOIN queries
   - Current implementation prioritizes simplicity and maintainability

---

## Testing Premium Filtering

### Scenario 1: Non-Premium User Views "Most Popular"

**Given**: User is not premium
**When**: User views "Most Popular" section
**Then**:
- Only lectures from non-premium speakers appear
- If all 9 trending lectures are from premium speakers, user sees empty list or fewer items
- No premium lectures are exposed in the API response

### Scenario 2: Non-Premium User Tries to Favorite Premium Lecture

**Given**: User is not premium
**When**: User attempts to favorite a premium lecture
**Then**:
- API returns error: "Cannot favorite premium content without premium access"
- Favorite is not created in database

### Scenario 3: Premium User Views All Content

**Given**: User has `isPremium = true`
**When**: User views any list (speakers, collections, lectures, trending, favorites)
**Then**:
- All content is visible, including premium content
- No filtering is applied

### Scenario 4: User Becomes Premium

**Given**: User favorites non-premium lectures, then upgrades to premium
**When**: User views favorites
**Then**:
- Previous favorites still visible
- Can now see and favorite premium lectures

---

## How to Mark Content as Premium

### Make a Speaker Premium:

```sql
UPDATE speakers SET is_premium = true WHERE id = 123;
```

**Result**: All of speaker's collections and lectures automatically become premium.

### Make a Speaker Non-Premium:

```sql
UPDATE speakers SET is_premium = false WHERE id = 123;
```

**Result**: All of speaker's collections and lectures automatically become accessible to all users.

### Make a User Premium:

```sql
UPDATE users SET is_premium = true WHERE clerk_id = 'user_abc123';
```

**Result**: User can now access all premium content.

---

## Files Modified

1. **Created**:
   - `src/main/java/com/elmify/backend/service/PremiumFilterService.java` (NEW)

2. **Updated**:
   - `src/main/java/com/elmify/backend/service/SpeakerService.java`
   - `src/main/java/com/elmify/backend/service/LectureService.java`
   - `src/main/java/com/elmify/backend/service/CollectionService.java`
   - `src/main/java/com/elmify/backend/service/FavoriteService.java`
   - `src/main/java/com/elmify/backend/entity/Collection.java` (added `isPremium()` method)
   - `src/main/java/com/elmify/backend/entity/Lecture.java` (added `isPremium()` method)

---

## API Response Examples

### Non-Premium User - GET /api/lectures/trending

```json
{
  "data": [
    {
      "id": 1,
      "title": "Introduction to Philosophy",
      "speakerName": "John Doe",
      "isPremium": false
    },
    {
      "id": 3,
      "title": "History of Rome",
      "speakerName": "Jane Smith",
      "isPremium": false
    }
  ]
}
```

**Note**: Lecture ID 2 from premium speaker is filtered out.

### Premium User - GET /api/lectures/trending

```json
{
  "data": [
    {
      "id": 1,
      "title": "Introduction to Philosophy",
      "speakerName": "John Doe",
      "isPremium": false
    },
    {
      "id": 2,
      "title": "Advanced Economics",
      "speakerName": "Dr. Premium",
      "isPremium": true
    },
    {
      "id": 3,
      "title": "History of Rome",
      "speakerName": "Jane Smith",
      "isPremium": false
    }
  ]
}
```

**Note**: All lectures visible, including premium ones.

---

## Future Improvements

1. **Database-Level Filtering**: Move filtering to SQL queries with JOINs for better performance
2. **Caching**: Cache premium status checks to reduce database queries
3. **Analytics**: Track premium content views and conversion rates
4. **Partial Access**: Allow preview/sample of premium lectures (e.g., first 5 minutes)
5. **Grace Period**: Allow recently-expired premium users limited access

---

## Troubleshooting

### Problem: Premium lectures still showing for non-premium users

**Check**:
1. Is backend restarted? Changes require server restart
2. Is user authentication working? Check `SecurityContextHolder`
3. Is speaker correctly marked as premium in database?
4. Check browser cache - old API responses may be cached

### Problem: Premium users cannot see premium content

**Check**:
1. Verify user's `isPremium` field in database: `SELECT * FROM users WHERE clerk_id = 'xxx'`
2. Check Clerk ID matches between frontend auth and backend
3. Verify JWT token is being sent correctly in requests

### Problem: Collections/Lectures not inheriting premium status

**Check**:
1. Verify `speaker` relationship is loaded (not lazy-loaded null)
2. Check `Collection.isPremium()` and `Lecture.isPremium()` methods exist
3. Ensure speaker's `isPremium` field is not null (use `false` as default)

---

## Summary

The premium filtering system ensures secure, consistent access control across all content types. By centralizing logic in `PremiumFilterService` and applying filters at the service layer, we prevent unauthorized access to premium content while maintaining clean, maintainable code.

**Key Takeaway**: Premium status is determined by the speaker, and filtering happens transparently at the service layer before data reaches the client.
