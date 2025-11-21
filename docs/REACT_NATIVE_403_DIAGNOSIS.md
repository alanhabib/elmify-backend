# 🔍 React Native 403 Error - Diagnosis & Fix

**Date:** November 21, 2025  
**Issue:** React Native getting 403 Forbidden  
**Root Cause:** ✅ **IDENTIFIED - Authentication, NOT CORS**

---

## ❌ CORS is NOT the Problem!

**Important:** React Native apps **DO NOT have CORS restrictions!**

CORS is a **browser security feature** only. React Native apps:

- ✅ Run natively on iOS/Android (not in a browser)
- ✅ Can make requests to any domain
- ✅ Don't need CORS configuration

---

## ✅ The Real Problem: Authentication

Looking at your backend logs:

```
WARN c.e.backend.config.SecurityConfig - Authentication failed: GET /api/v1/users/me from IP: 92.35.108.32
WARN c.e.b.e.GlobalExceptionHandler - Access denied: Access Denied [traceId: 9d710569]
```

**The issue:**

- Your React Native app is calling `/api/v1/users/me` (authenticated endpoint)
- The request **doesn't have a valid JWT token**
- Backend correctly returns 403 Forbidden

---

## 🔍 Diagnosis: Which 403 Error Do You Have?

### Scenario 1: Authenticated Endpoints (Most Likely)

**Endpoints that require JWT:**

- `/api/v1/users/me` ← Your logs show this
- `/api/v1/playback/*`
- `/api/v1/saved-lectures/*`
- Any POST/PUT/DELETE request

**Solution:** Add JWT token to requests

```typescript
// In your React Native app
const token = await clerk.session?.getToken();

fetch('https://elmify-backend-production.up.railway.app/api/v1/users/me', {
    headers: {
        'Authorization': `Bearer ${token}`
    }
});
```

### Scenario 2: Public Endpoints (Should Work)

**Endpoints that DON'T require JWT:**

- `/api/v1/speakers` (GET)
- `/api/v1/collections` (GET)
- `/api/v1/lectures` (GET)
- `/api/v1/lectures/*/stream-url` (GET)

**Test:** Try this in your app

```typescript
// This should work WITHOUT a token
fetch('https://elmify-backend-production.up.railway.app/api/v1/speakers')
    .then(res => res.json())
    .then(console.log);
```

---

## 🔧 Quick Diagnostic Tests

Run these in your React Native app:

### Test 1: Public Endpoint (Should Work)

```typescript
console.log('Test 1: Public endpoint');
fetch('https://elmify-backend-production.up.railway.app/api/v1/speakers')
    .then(res => {
        console.log('Status:', res.status);
        return res.json();
    })
    .then(data => console.log('Speakers:', data))
    .catch(err => console.error('Error:', err));
```

**Expected:** Status 200, list of speakers

### Test 2: Authenticated Endpoint WITHOUT Token (Should Fail)

```typescript
console.log('Test 2: Authenticated endpoint without token');
fetch('https://elmify-backend-production.up.railway.app/api/v1/users/me')
    .then(res => {
        console.log('Status:', res.status); // Should be 401 or 403
        return res.text();
    })
    .then(data => console.log('Response:', data));
```

**Expected:** Status 401, error message

### Test 3: Authenticated Endpoint WITH Token (Should Work)

```typescript
console.log('Test 3: Authenticated endpoint with token');
const token = await clerk.session?.getToken();

fetch('https://elmify-backend-production.up.railway.app/api/v1/users/me', {
    headers: {
        'Authorization': `Bearer ${token}`
    }
})
    .then(res => {
        console.log('Status:', res.status); // Should be 200
        return res.json();
    })
    .then(data => console.log('User:', data));
```

**Expected:** Status 200, your user data

---

## 🎯 Most Likely Solution

Your React Native app is probably **missing the Authorization header** on some requests.

### Check Your API Service

Find where you make API calls (probably in a service file like `api.ts` or `apiClient.ts`):

```typescript
// ❌ WRONG - Missing Authorization header
const response = await fetch(`${API_URL}/api/v1/users/me`);

// ✅ CORRECT - With Authorization header
const token = await clerk.session?.getToken();
const response = await fetch(`${API_URL}/api/v1/users/me`, {
    headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    }
});
```

### Create a Helper Function

```typescript
// api/client.ts
import {useAuth} from '@clerk/clerk-expo';

export const createAuthenticatedRequest = async (
    url: string,
    options: RequestInit = {}
) => {
    const {getToken} = useAuth();
    const token = await getToken();

    return fetch(url, {
        ...options,
        headers: {
            ...options.headers,
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    });
};

// Usage:
const response = await createAuthenticatedRequest(
    'https://elmify-backend-production.up.railway.app/api/v1/users/me'
);
```

---

## 📋 Backend Security Configuration (Current)

Your backend is correctly configured:

| Endpoint Type                     | Authentication Required | Status       |
|-----------------------------------|-------------------------|--------------|
| GET /api/v1/speakers              | ❌ No                    | Public       |
| GET /api/v1/collections           | ❌ No                    | Public       |
| GET /api/v1/lectures              | ❌ No                    | Public       |
| GET /api/v1/lectures/*/stream-url | ❌ No                    | Public       |
| GET /api/v1/users/me              | ✅ Yes                   | Requires JWT |
| GET /api/v1/playback/*            | ✅ Yes                   | Requires JWT |
| POST /api/v1/**                   | ✅ Yes                   | Requires JWT |
| PUT /api/v1/**                    | ✅ Yes                   | Requires JWT |
| DELETE /api/v1/**                 | ✅ Yes                   | Requires JWT |

---

## 🐛 Common React Native Auth Issues

### Issue 1: Token Not Retrieved

```typescript
// ❌ WRONG - getToken() not awaited
const token = clerk.session?.getToken(); // Returns a Promise!

// ✅ CORRECT
const token = await clerk.session?.getToken();
```

### Issue 2: Token Expired

```typescript
// Add error handling
const token = await clerk.session?.getToken();
if (!token) {
    console.error('No token available - user not signed in?');
    // Redirect to login
}
```

### Issue 3: Using Wrong Token

```typescript
// ❌ WRONG - Using session ID instead of token
const sessionId = clerk.session?.id;

// ✅ CORRECT - Using JWT token
const token = await clerk.session?.getToken();
```

---

## 🔍 Debug Your React Native App

Add logging to see what's happening:

```typescript
const makeAuthenticatedRequest = async (url: string) => {
    const token = await clerk.session?.getToken();

    console.log('🔑 Token available:', !!token);
    console.log('🔑 Token preview:', token?.substring(0, 20) + '...');
    console.log('📡 Request URL:', url);

    const response = await fetch(url, {
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    });

    console.log('📬 Response status:', response.status);
    console.log('📬 Response OK:', response.ok);

    if (!response.ok) {
        const errorText = await response.text();
        console.error('❌ Error response:', errorText);
    }

    return response;
};
```

---

## ✅ Action Items

1. **Test public endpoints** (should work without any changes)
   ```typescript
   fetch('https://elmify-backend-production.up.railway.app/api/v1/speakers')
   ```

2. **Check authenticated requests** have Authorization header
   ```typescript
   headers: { 'Authorization': `Bearer ${token}` }
   ```

3. **Verify token is being retrieved**
   ```typescript
   const token = await clerk.session?.getToken();
   console.log('Token:', token);
   ```

4. **Share your API client code** if still having issues

---

## 📊 Summary

| Item                  | Status                                            |
|-----------------------|---------------------------------------------------|
| CORS issue            | ❌ Not applicable (React Native doesn't have CORS) |
| Authentication issue  | ✅ Most likely cause                               |
| Backend configuration | ✅ Correct                                         |
| Frontend code         | ⏳ Needs review                                    |

---

## 🆘 Need Help?

Share:

1. Your API client/service code (where you make fetch requests)
2. Console logs from the diagnostic tests above
3. Are you using Clerk? Show how you get the token

**The 403 is NOT a CORS issue - it's an authentication issue!** 🎯

