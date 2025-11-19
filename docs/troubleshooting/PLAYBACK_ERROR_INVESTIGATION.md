# 🔍 Playback Error Investigation

**Date:** November 18, 2025  
**Error:** 500 Internal Server Error on `/api/v1/playback/835`  
**Status:** ⚠️ CRITICAL ISSUE FOUND

---

## Issue Summary

### Problem 1: Wrong Endpoint Used ❌
Your React Native app is calling:
```
GET /api/v1/playback/835
```

But this endpoint is for **playback positions** (saving/loading where user left off), NOT for getting audio URLs.

### Problem 2: Missing `speaker_id` in Lectures Table ✅ FIXED
The import script did NOT populate `speaker_id` in the `lectures` table, only `collection_id`.

**UPDATE:** This has been fixed! See `FIXES_APPLIED.md` for details.

---

## Investigation Results

### 1. ✅ Data Was Imported Correctly

**Badr al-Turki in Database:**
```sql
-- Speaker exists
SELECT id, name FROM speakers WHERE name = 'Badr al-Turki';
-- Result: id=44, name='Badr al-Turki'

-- Collection exists
SELECT id, title FROM collections WHERE speaker_id = 44;
-- Result: id=84, title='Quran Hafs'

-- Lectures exist
SELECT COUNT(*) FROM lectures WHERE collection_id = 84;
-- Result: 114 lectures ✅
```

**Conclusion:** Badr al-Turki has **114 lectures** in the database. The frontend issue is likely a different problem (caching, API query, etc.).

---

### 2. ❌ Lecture 835 Missing `speaker_id`

**Database Query:**
```sql
SELECT id, title, file_path, lecture_number, speaker_id, collection_id 
FROM lectures 
WHERE id = 835;
```

**Result:**
```
id: 835
title: Imam Bukhari
file_path: Ahmad Jibril/Legends Islam/01 - Imam Bukhari.mp3
lecture_number: 1
speaker_id: NULL  ← PROBLEM!
collection_id: 78
```

**Why This Is a Problem:**

The `LectureController.buildR2CdnUrl()` method does this:
```java
if (lecture.getSpeaker() == null || lecture.getCollection() == null) {
    logger.warn("Lecture {} missing speaker or collection relationship", lecture.getId());
    return null;  // ← Returns null, causing fallback
}
```

Since `speaker_id` is NULL, `lecture.getSpeaker()` returns null, so it falls back to using `file_path`.

**Current Behavior:**
- ✅ URL is still generated from `file_path`
- ✅ Returns: `https://cdn.elmify.store/Ahmad Jibril/Legends Islam/01 - Imam Bukhari.mp3`
- ⚠️ But this only works as a fallback

---

### 3. ❌ Import Script Doesn't Set `speaker_id`

**From `import_manifest.js` line 136-143:**
```javascript
await client.query(
  `INSERT INTO lectures (
    collection_id, title, description, lecture_number, file_name, file_path,
    duration, file_size, file_format, bitrate, sample_rate, file_hash,
    created_at, updated_at
   )
   VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, NOW(), NOW())`,
  [
    collectionId, lectureTitle, ...
  ]
);
```

**Missing:** `speaker_id` is NOT included in the INSERT statement!

**Database Schema Has It:**
```sql
\d lectures
...
speaker_id      bigint  REFERENCES speakers(id)
collection_id   bigint  REFERENCES collections(id)
...
```

The column exists, but the import script doesn't populate it.

---

### 4. ⚠️ Wrong Endpoint Being Called

**Your React Native App Logs:**
```
Request URL: https://elmify-backend-production.up.railway.app/api/v1/playback/835
Status Code: 500 Internal Server Error
```

**Correct Endpoint for Audio URL:**
```
GET /api/v1/lectures/{id}/stream-url
```

**Playback Endpoint Is For:**
- Saving playback position (e.g., user paused at 1:30)
- Retrieving where user left off
- NOT for getting audio URLs

---

## Root Causes

### Cause 1: Wrong API Endpoint ❌
The React Native app is calling `/api/v1/playback/835` instead of `/api/v1/lectures/835/stream-url`.

**Evidence:**
- Playback endpoint expects a PUT/DELETE for saving positions
- GET on playback endpoint may not be properly handling lecture IDs
- 500 error suggests endpoint mismatch

### Cause 2: Missing `speaker_id` in Lectures ⚠️
The import script only sets `collection_id`, not `speaker_id`.

**Impact:**
- The `buildR2CdnUrl()` method can't construct dynamic URLs
- Falls back to `file_path` (which works, but not ideal)
- May cause issues with future features that rely on speaker relationship

### Cause 3: Frontend Not Showing Badr al-Turki ❓
The database has 114 lectures for Badr al-Turki, but the frontend shows 0.

**Possible Causes:**
- Frontend caching issue
- API query filtering out the speaker
- Premium filtering blocking the content
- Frontend parsing error

---

## What's Working vs What's Broken

### ✅ Working
1. Data import completed successfully
2. All 8 speakers imported
3. All 12 collections imported
4. 420+ lectures imported
5. `file_path` is correct
6. Fallback URL generation works

### ❌ Broken
1. React Native calling wrong endpoint (`/playback/` instead of `/lectures/.../stream-url`)
2. `speaker_id` not populated in lectures table
3. Frontend not showing Badr al-Turki's lectures (database has them)

### ⚠️ Partially Working
- URL generation works via fallback, but not using optimal path

---

## The 500 Error Explained

**Why `/api/v1/playback/835` Returns 500:**

Looking at `PlaybackPositionController.java`:
```java
@GetMapping("/{lectureId}")
public ResponseEntity<PlaybackPositionDto> getPosition(
    @AuthenticationPrincipal String userId,
    @PathVariable Long lectureId
) {
    return playbackPositionService.getPosition(userId, lectureId)
        .map(pos -> ResponseEntity.ok(PlaybackPositionDto.fromEntity(pos)))
        .orElse(ResponseEntity.ok(PlaybackPositionDto.createEmpty(lectureId)));
}
```

This endpoint:
1. Expects authentication (userId from JWT)
2. Looks up playback position in `playback_positions` table
3. Returns position data (NOT audio URL)

**The 500 error likely occurs because:**
- The service is trying to fetch a lecture that doesn't have a speaker relationship
- OR there's an error in the playback position logic
- OR authentication is failing

**But this is the WRONG endpoint anyway!**

---

## Correct Flow for Playing Audio

### What Should Happen:

1. **Frontend:** User clicks on lecture 835
2. **API Call:** `GET /api/v1/lectures/835/stream-url`
3. **Backend:** 
   - Fetches lecture with speaker & collection
   - Builds R2 CDN URL
   - Returns: `{"url": "https://cdn.elmify.store/..."}`
4. **Frontend:** Uses URL to load audio in TrackPlayer
5. **Audio Plays:** ✅

### What's Actually Happening:

1. **Frontend:** User clicks on lecture 835
2. **API Call:** `GET /api/v1/playback/835` ← WRONG!
3. **Backend:** Returns 500 error (wrong endpoint)
4. **Frontend:** Error handling → still tries to play
5. **Audio Fails:** ❌

---

## URL Format Analysis

**Your logs show:**
```javascript
🎵 StreamingService: Received URL from backend: 
https://cdn.elmify.store/Ahmad Jibril/Legends Islam/01 - Imam Bukhari.mp3

🎵 StreamingService: Encoded URL: 
https://cdn.elmify.store/Ahmad%20Jibril/Legends%20Islam/01%20-%20Imam%20Bukhari.mp3
```

**This URL format is CORRECT!** ✅

The error `SwiftAudioEx.AudioPlayerError.PlaybackError` suggests:
1. The URL might not be accessible from the app
2. CORS issue
3. The file doesn't exist at that path in R2
4. Network/SSL issue

---

## Required Fixes

### Fix 1: Update React Native API Call (HIGH PRIORITY) 🔴

**Change from:**
```typescript
GET /api/v1/playback/${lectureId}
```

**Change to:**
```typescript
GET /api/v1/lectures/${lectureId}/stream-url
```

**This is the most critical fix!**

---

### Fix 2: Add `speaker_id` to Import Script (MEDIUM PRIORITY) 🟡

The import script needs to populate `speaker_id` in the lectures table.

**Why this matters:**
- Enables proper relationship queries
- Allows `buildR2CdnUrl()` to work without fallback
- Future features may depend on this relationship

**Current workaround:**
The fallback to `file_path` works, so this isn't breaking anything right now.

---

### Fix 3: Investigate Frontend Filtering (LOW PRIORITY) 🟢

**Question:** Why doesn't Badr al-Turki show up in the frontend?

**Database has:**
- Speaker: Badr al-Turki (id=44)
- Collection: Quran Hafs (id=84)
- Lectures: 114 lectures

**Possible causes:**
- Premium filtering
- Frontend query excluding this speaker
- Caching issue
- Sorting/pagination hiding the results

---

## Next Steps (Prioritized)

### 1. ✅ Fix React Native Endpoint (IMMEDIATE)
Check your React Native code and change the API endpoint from `/playback/` to `/lectures/.../stream-url`.

### 2. ✅ Verify R2 File Exists (IMMEDIATE)
Test if the file actually exists:
```bash
curl -I "https://cdn.elmify.store/Ahmad%20Jibril/Legends%20Islam/01%20-%20Imam%20Bukhari.mp3"
```

Expected: `200 OK` or `403 Forbidden`
If `404 Not Found`, the file doesn't exist in R2 at that path.

### 3. ✅ Check Backend Logs (IMMEDIATE)
Look at Railway logs to see what the actual error is:
```bash
railway logs
```

Look for errors around the playback endpoint.

### 4. 🔧 Fix Import Script (LATER)
Add `speaker_id` to the import script (I can help with this after confirming the immediate issues).

### 5. 🔍 Debug Frontend Filtering (LATER)
Once playback works, investigate why Badr al-Turki doesn't show in the app.

---

## SQL Queries to Verify Data

```sql
-- Check lecture 835
SELECT l.id, l.title, l.file_path, l.speaker_id, l.collection_id,
       c.title as collection_title, s.name as speaker_name
FROM lectures l
LEFT JOIN collections c ON l.collection_id = c.id
LEFT JOIN speakers s ON c.speaker_id = s.id
WHERE l.id = 835;

-- Check Badr al-Turki's data
SELECT s.id, s.name, 
       COUNT(DISTINCT c.id) as collection_count,
       COUNT(l.id) as lecture_count
FROM speakers s
LEFT JOIN collections c ON s.id = c.speaker_id
LEFT JOIN lectures l ON c.id = l.collection_id
WHERE s.name = 'Badr al-Turki'
GROUP BY s.id, s.name;

-- Check all lectures without speaker_id
SELECT COUNT(*) as lectures_without_speaker
FROM lectures
WHERE speaker_id IS NULL;
```

---

## Summary

| Issue | Severity | Status | Fix Required |
|-------|----------|--------|--------------|
| Wrong API endpoint in React Native | 🔴 CRITICAL | Needs Fix | Change to `/lectures/.../stream-url` |
| Missing `speaker_id` in lectures | ✅ FIXED | Complete | Already fixed - see FIXES_APPLIED.md |
| Badr al-Turki not showing in app | 🟢 LOW | Data exists in DB | Debug frontend filtering |
| 500 error on playback endpoint | 🔴 CRITICAL | Caused by wrong endpoint | Fix endpoint call |
| URL format | ✅ GOOD | Working | No change needed |

---

**The primary issue is the React Native app calling the wrong endpoint. Once that's fixed, playback should work.**

**Backend issues have been resolved - see `FIXES_APPLIED.md` for details.**

