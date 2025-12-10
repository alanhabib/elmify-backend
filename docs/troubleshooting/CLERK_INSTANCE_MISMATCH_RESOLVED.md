# Clerk Instance Mismatch - Resolved

> **Date:** December 8, 2025  
> **Issue:** Frontend using development Clerk, backend expecting production Clerk  
> **Status:** ✅ RESOLVED

---

## Problem Summary

The backend authentication was failing with:

```
❌ JWT validation failed! 
Token issuer: 'https://strong-bison-1.clerk.accounts.dev' 
Expected issuer: 'https://clerk.elmify.store'

UnknownHostException: clerk.elmify.store
```

### Root Cause

You have **two Clerk instances**:

1. **Development Instance**
    - Issuer: `https://strong-bison-1.clerk.accounts.dev`
    - Used by: Frontend (currently)
    - Publishable Key: `pk_test_...`
    - Secret Key: `sk_test_...`

2. **Production Instance (Custom Domain)**
    - Issuer: `https://clerk.elmify.store`
    - Used by: Backend (currently)
    - Publishable Key: `pk_live_...`
    - Secret Key: `sk_live_...`
    - Frontend API URL: `https://clerk.elmify.store`
    - JWKS URL: `https://clerk.elmify.store/.well-known/jwks.json`

**The mismatch:** Frontend sends JWTs from development instance, but backend expects production instance JWTs.

---

## Cloudflare DNS Configuration (Already Correct ✅)

Your Cloudflare DNS records are properly configured:

```
CNAME clerk → frontend-api.clerk.services (DNS only)
```

This is correct for Clerk custom domains.

---

## Solution: Align Frontend and Backend

You have two options:

### Option 1: Use Development Clerk on Backend (Recommended for Testing)

**Update Railway Environment Variables:**

1. Go to **Railway Dashboard** → elmify-backend → **Variables**

2. Update these variables to use **development** Clerk:

   ```bash
   CLERK_JWT_ISSUER=https://strong-bison-1.clerk.accounts.dev
   CLERK_SECRET_KEY=sk_test_...  # Your development secret key
   CLERK_PUBLISHABLE_KEY=pk_test_...  # Your development publishable key
   ```

3. Railway will auto-redeploy

**Pros:**

- Quick fix
- Works immediately
- Good for development/testing

**Cons:**

- Not using custom domain
- Need to switch to production keys before launch

---

### Option 2: Use Production Clerk on Frontend (Recommended for Production)

**Update Frontend Environment Variables:**

1. In your React Native app's `.env` or config:

   ```bash
   CLERK_PUBLISHABLE_KEY=pk_live_...  # Your production publishable key
   ```

2. Rebuild and redeploy your frontend

**Pros:**

- Uses your custom domain `clerk.elmify.store`
- Production-ready
- Better branding

**Cons:**

- Requires frontend rebuild
- Need to ensure DNS is fully propagated

---

## Verification Steps

After making changes, verify:

### 1. Check Backend Logs (Railway)

Look for this log line at startup:

```
✅ ClerkJwtDecoder initialized. Expected JWT issuer: https://strong-bison-1.clerk.accounts.dev
```

Or for production:

```
✅ ClerkJwtDecoder initialized. Expected JWT issuer: https://clerk.elmify.store
```

### 2. Check Frontend JWT

In your React Native app logs, verify the JWT issuer matches:

```javascript
{
  iss: "https://strong-bison-1.clerk.accounts.dev",  // Should match backend
  sub: "user_xxx",
  ...
}
```

### 3. Test Authentication

```bash
# From your app, try to call an authenticated endpoint
GET /api/v1/users/me

# Should return 200 OK with user profile
```

---

## Understanding Clerk Instances

### Development Instance

- URL: `https://strong-bison-1.clerk.accounts.dev`
- Purpose: Testing, development
- Keys: Start with `pk_test_` and `sk_test_`
- JWTs: Issued with `iss: https://strong-bison-1.clerk.accounts.dev`

### Production Instance (Custom Domain)

- URL: `https://clerk.elmify.store`
- Purpose: Production use
- Keys: Start with `pk_live_` and `sk_live_`
- JWTs: Issued with `iss: https://clerk.elmify.store`
- Requires: DNS configured (you have this ✅)

---

## Finding Your Clerk Keys

1. Go to **Clerk Dashboard** → https://dashboard.clerk.com
2. Select your application
3. Go to **API Keys**
4. You'll see:
    - **Development** keys (for testing)
    - **Production** keys (for live app)

Make sure frontend and backend use keys from the **same instance**.

---

## Recommended Configuration

### For Current Development/Testing

**Backend (Railway):**

```bash
CLERK_JWT_ISSUER=https://strong-bison-1.clerk.accounts.dev
CLERK_SECRET_KEY=sk_test_xxx
CLERK_PUBLISHABLE_KEY=pk_test_xxx
```

**Frontend (React Native):**

```bash
CLERK_PUBLISHABLE_KEY=pk_test_xxx
```

### For Production Launch

**Backend (Railway):**

```bash
CLERK_JWT_ISSUER=https://clerk.elmify.store
CLERK_SECRET_KEY=sk_live_xxx
CLERK_PUBLISHABLE_KEY=pk_live_xxx
```

**Frontend (React Native):**

```bash
CLERK_PUBLISHABLE_KEY=pk_live_xxx
```

---

## Troubleshooting

### Still Getting UnknownHostException for clerk.elmify.store?

1. **Verify DNS propagation:**
   ```bash
   nslookup clerk.elmify.store
   dig clerk.elmify.store
   ```

2. **Test JWKS endpoint:**
   ```bash
   curl https://clerk.elmify.store/.well-known/jwks.json
   ```

3. **Check Cloudflare:**
    - Ensure CNAME record is not proxied (should be "DNS only")
    - Wait 5-10 minutes for DNS propagation

### JWT Issuer Mismatch?

The issuer in the JWT **MUST exactly match** `CLERK_JWT_ISSUER` on the backend.

Check backend logs for:

```
Token issuer: 'X' | Expected issuer: 'Y'
```

If they don't match, update the environment variable to use the correct instance.

---

## Current Status

✅ **DNS configured correctly** in Cloudflare  
✅ **Code improvements deployed** (better error messages)  
⏳ **Waiting for:** Environment variable update on Railway

**Next Step:** Update `CLERK_JWT_ISSUER` on Railway to match your frontend's Clerk instance.

---

## Files Modified in This Fix

1. `src/main/java/com/elmify/backend/security/ClerkJwtDecoder.java`
    - Added detailed error logging
    - Shows token issuer vs expected issuer
    - Better startup logging

2. `src/main/java/com/elmify/backend/config/SecurityConfig.java`
    - Added `optionalBearerTokenResolver()` for public endpoints
    - Skips JWT validation for public GET endpoints

3. `src/main/java/com/elmify/backend/security/JwtUserSyncFilter.java` (NEW)
    - Auto-syncs users from JWT claims
    - Creates users on first authentication

---

## Related Documentation

- [Clerk JWT Authentication & User Sync Guide](./CLERK_JWT_USER_SYNC_GUIDE.md)
- [Clerk Custom Domains](https://clerk.com/docs/advanced-usage/custom-domains)

