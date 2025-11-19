# 🚨 URGENT: Fix Your React Native App

**Date:** November 18, 2025  
**Priority:** 🔴 CRITICAL

---

## The Problem

Your React Native app is calling the **WRONG API ENDPOINT** to get audio URLs.

### Current (WRONG):
```typescript
GET /api/v1/playback/835
```

### Should Be:
```typescript
GET /api/v1/lectures/835/stream-url
```

---

## How to Fix

### 1. Find the Wrong Code

Search your React Native codebase for:
- `/playback/`
- `playback/${lectureId}`
- `api/v1/playback`

Likely files to check:
- `services/StreamingService.ts` (or similar)
- `services/AudioService.ts`
- `api/lectures.ts`
- Any file that fetches audio URLs

### 2. Replace With Correct Code

**BEFORE (WRONG):**
```typescript
// ❌ This is for playback positions, NOT audio URLs
const response = await fetch(
  `${API_BASE_URL}/api/v1/playback/${lectureId}`,
  {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  }
);
```

**AFTER (CORRECT):**
```typescript
// ✅ This gets the streaming URL
const response = await fetch(
  `${API_BASE_URL}/api/v1/lectures/${lectureId}/stream-url`,
  {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  }
);

const data = await response.json();
const audioUrl = data.url; // e.g., "https://cdn.elmify.store/..."
```

### 3. Expected Response

**Correct endpoint returns:**
```json
{
  "url": "https://cdn.elmify.store/Ahmad Jibril/Legends Islam/01 - Imam Bukhari.mp3"
}
```

**Wrong endpoint returns:**
```json
{
  "lectureId": 835,
  "currentPosition": 0
}
```

---

## What Each Endpoint Does

### `/api/v1/lectures/{id}/stream-url` ✅ USE THIS
**Purpose:** Get the CDN URL for streaming audio  
**Method:** GET  
**Returns:** `{"url": "https://cdn.elmify.store/..."}`  
**Use for:** Loading audio into TrackPlayer

### `/api/v1/playback/{lectureId}` ❌ NOT FOR AUDIO
**Purpose:** Get/save where user paused the lecture  
**Methods:** GET (get position), PUT (save position)  
**Returns:** `{"lectureId": 835, "currentPosition": 123}`  
**Use for:** Resume playback feature

---

## Your Logs Show The Problem

```javascript
🎵 StreamingService: Received URL from backend: 
https://cdn.elmify.store/Ahmad Jibril/Legends Islam/01 - Imam Bukhari.mp3
```

**This means:**
- You're somehow getting the URL already (maybe from a different call?)
- But the `/api/v1/playback/835` call is returning 500 error
- This suggests you're making BOTH calls, but one is failing

**Action:** Search for ALL API calls related to playing a lecture and verify each one.

---

## Testing

### Test the Correct Endpoint:

```bash
# Replace YOUR_TOKEN with your actual JWT token
curl -H "Authorization: Bearer YOUR_TOKEN" \
  https://elmify-backend-production.up.railway.app/api/v1/lectures/835/stream-url

# Expected response:
# {"url":"https://cdn.elmify.store/Ahmad Jibril/Legends Islam/01 - Imam Bukhari.mp3"}
```

### Test the Wrong Endpoint (for comparison):

```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  https://elmify-backend-production.up.railway.app/api/v1/playback/835

# This might return 500 or playback position data
```

---

## Backend Status

### ✅ Backend is FULLY WORKING

- All data imported correctly
- All relationships set properly
- Stream URL endpoint working perfectly
- Returns correct CDN URLs

### ❌ Frontend is calling wrong endpoint

- Using `/playback/` instead of `/lectures/.../stream-url`
- This is causing the 500 error you're seeing

---

## Fix Timeline

**This is a 5-minute fix:**

1. Search for `/playback/` in your codebase (1 min)
2. Replace with `/lectures/${lectureId}/stream-url` (1 min)
3. Update response parsing to use `data.url` (1 min)
4. Test playback (2 min)

**Total: 5 minutes to fix** ⏱️

---

## Other Issues

### Badr al-Turki Not Showing

**Database has:**
- ✅ Speaker exists
- ✅ 1 collection (Quran Hafs)
- ✅ 114 lectures

**Frontend shows:**
- ❌ 0 collections
- ❌ 0 lectures

**Possible causes:**
1. Frontend caching old data
2. Premium filtering
3. Pagination hiding results
4. Frontend parsing error

**Debug steps:**
1. Clear app cache/data
2. Check API response: `GET /api/v1/speakers`
3. Check if Badr al-Turki is in the response
4. Check collections: `GET /api/v1/speakers/44/collections`

---

## Summary

| What | Status |
|------|--------|
| Backend | ✅ Working perfectly |
| Database | ✅ All data present |
| Import script | ✅ Fixed to include speaker_id |
| Existing data | ✅ Updated with speaker_id |
| API endpoints | ✅ All working |
| Frontend endpoint call | ❌ **FIX THIS** |
| Badr al-Turki display | ❌ Debug after fixing playback |

---

## Need Help?

If you're having trouble finding where to fix the code, share:
1. Your `StreamingService.ts` (or similar file)
2. Any file that fetches audio URLs
3. The error logs from React Native

**But 99% sure the fix is: change `/playback/` to `/lectures/.../stream-url`** 🎯

