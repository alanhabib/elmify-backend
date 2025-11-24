# Railway Deployment Troubleshooting

## Current Issue: Healthcheck Timeout

**Problem**: Application builds successfully but fails healthcheck after 5 minutes
**Root Cause**: Missing environment variables causing application startup failure

---

## Required Environment Variables

### Critical (App won't start without these)

```bash
# Database (Railway PostgreSQL)
DATABASE_URL=jdbc:postgresql://[host]:[port]/railway
DB_USERNAME=postgres
DB_PASSWORD=[railway_generated]

# Clerk Authentication
CLERK_SECRET_KEY=sk_live_...
CLERK_PUBLISHABLE_KEY=pk_live_...
CLERK_JWT_ISSUER=https://[your-app].clerk.accounts.dev

# Cloudflare R2 Storage
R2_ENDPOINT=https://[account-id].r2.cloudflarestorage.com
R2_BUCKET_NAME=your-bucket-name
R2_ACCESS_KEY=your-r2-access-key
R2_SECRET_KEY=your-r2-secret-key
R2_REGION=auto

# Server
PORT=8080
SPRING_PROFILES_ACTIVE=prod
```

### Optional (App will work without, but with warnings)

```bash
# Redis (for playlist manifest caching)
REDIS_HOST=redis.railway.internal
REDIS_PORT=6379
REDIS_PASSWORD=[if_using_external_redis]
REDIS_SSL=false

# Admin
ADMIN_EMAILS=dalanhabib@gmail.com

# CORS (for web clients)
CORS_ALLOWED_ORIGINS=https://elmify.store,https://www.elmify.store
```

---

## Deployment Steps

### 1. Add PostgreSQL Database

```bash
# In Railway project
railway add postgres

# This automatically sets:
# - DATABASE_URL
# - POSTGRES_USER
# - POSTGRES_PASSWORD
```

**Note**: Railway's `DATABASE_URL` format is different from Spring Boot format.

### 2. Transform DATABASE_URL

Railway provides: `postgres://user:pass@host:port/db`
Spring needs: `jdbc:postgresql://host:port/db`

**Option A: Use Railway's built-in transformation**
Set these variables in Railway:
```bash
DATABASE_URL=${{Postgres.DATABASE_URL}}
DB_USERNAME=${{Postgres.POSTGRES_USER}}
DB_PASSWORD=${{Postgres.POSTGRES_PASSWORD}}
```

Railway will auto-expand `${{Postgres.*}}` references.

**Option B: Manual transformation** (if Railway doesn't auto-transform)
```bash
# Extract from Railway's DATABASE_URL and set manually
DATABASE_URL=jdbc:postgresql://[extract_host_port_db_from_railway]
DB_USERNAME=[extract_user]
DB_PASSWORD=[extract_password]
```

### 3. Add Required Environment Variables

In Railway dashboard or via CLI:

```bash
# Clerk (get from https://dashboard.clerk.com)
railway variables set CLERK_SECRET_KEY="sk_live_..."
railway variables set CLERK_PUBLISHABLE_KEY="pk_live_..."
railway variables set CLERK_JWT_ISSUER="https://your-app.clerk.accounts.dev"

# R2 (get from Cloudflare dashboard)
railway variables set R2_ENDPOINT="https://[account-id].r2.cloudflarestorage.com"
railway variables set R2_BUCKET_NAME="your-bucket"
railway variables set R2_ACCESS_KEY="your-key"
railway variables set R2_SECRET_KEY="your-secret"
railway variables set R2_REGION="auto"

# Server
railway variables set PORT="8080"
railway variables set SPRING_PROFILES_ACTIVE="prod"
```

### 4. (Optional) Add Redis

```bash
railway add redis

# This automatically sets REDIS_HOST, REDIS_PORT, etc.
# You may need to set these manually:
railway variables set REDIS_HOST="${{Redis.REDIS_HOST}}"
railway variables set REDIS_PORT="${{Redis.REDIS_PORT}}"
railway variables set REDIS_PASSWORD="${{Redis.REDIS_PASSWORD}}"
```

### 5. Deploy

```bash
railway up
```

---

## Verification Steps

### 1. Check Build Logs

```bash
railway logs --deployment

# Look for:
✅ [INFO] BUILD SUCCESS
✅ Successfully built [image-id]
✅ Successfully tagged [image]
```

### 2. Check Startup Logs

```bash
railway logs

# Look for:
✅ Started ElmifyApplication in X seconds
✅ Tomcat started on port(s): 8080 (http)
✅ Redis caching enabled (or warning if disabled)
❌ DATABASE CONNECTION FAILED (if this appears, DB config is wrong)
❌ Failed to configure Clerk JWT decoder (if this appears, Clerk config is wrong)
```

### 3. Test Health Endpoint

Once deployed:
```bash
curl https://your-app.railway.app/actuator/health
```

**Expected response:**
```json
{
  "status": "UP"
}
```

### 4. Test Playlist Manifest Endpoint

```bash
curl -X POST https://your-app.railway.app/api/playlists/manifest \
  -H "Authorization: Bearer YOUR_JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "collectionId": "1",
    "lectureIds": ["1", "2"]
  }'
```

---

## Common Issues

### Issue 1: "Failed to configure a DataSource"

**Cause**: Missing DATABASE_URL, DB_USERNAME, or DB_PASSWORD
**Fix**:
1. Verify PostgreSQL is added: `railway add postgres`
2. Check variables are set: `railway variables`
3. Verify DATABASE_URL format starts with `jdbc:postgresql://`

### Issue 2: "Failed to configure Clerk JWT decoder"

**Cause**: Missing or invalid CLERK_SECRET_KEY, CLERK_JWT_ISSUER
**Fix**:
1. Get keys from https://dashboard.clerk.com
2. Ensure CLERK_JWT_ISSUER matches your Clerk instance URL
3. Use production keys (sk_live_...) not test keys

### Issue 3: "R2 connection failed" in logs

**Cause**: Missing or invalid R2 credentials
**Fix**:
1. Get credentials from Cloudflare R2 dashboard
2. Verify R2_ENDPOINT format: `https://[account-id].r2.cloudflarestorage.com`
3. Test credentials locally first

### Issue 4: "Redis not configured" warning

**This is OK!** The app works without Redis, just slower.
**To enable**: `railway add redis` and redeploy

### Issue 5: Healthcheck still timing out after 5 minutes

**Cause**: Application is crashing on startup (not reaching healthy state)
**Debug**:
```bash
railway logs --deployment

# Look for stack traces indicating:
- Database connection errors
- Authentication configuration errors
- Missing bean errors
- Flyway migration errors
```

**Common fixes**:
- Set `spring.flyway.validate-on-migrate=false` in production
- Ensure all required beans have their dependencies configured
- Check for circular dependency issues

---

## Quick Checklist

Before deploying, verify:

- [ ] PostgreSQL database added to Railway project
- [ ] DATABASE_URL, DB_USERNAME, DB_PASSWORD set correctly (jdbc:postgresql format)
- [ ] CLERK_SECRET_KEY, CLERK_PUBLISHABLE_KEY, CLERK_JWT_ISSUER set
- [ ] R2_ENDPOINT, R2_BUCKET_NAME, R2_ACCESS_KEY, R2_SECRET_KEY set
- [ ] PORT=8080 set
- [ ] SPRING_PROFILES_ACTIVE=prod set
- [ ] (Optional) Redis added if you want caching
- [ ] Application builds locally: `./mvnw clean install`
- [ ] Dockerfile builds locally: `docker build -t elmify-test .`

---

## Next Steps After Successful Deployment

1. Test health endpoint: `curl https://your-app.railway.app/actuator/health`
2. Test playlist manifest endpoint with real JWT token
3. Update frontend `.env` with Railway URL: `API_BASE_URL=https://your-app.railway.app`
4. Test full integration: frontend → backend → R2 streaming
5. (Later) Add Redis for 10x performance boost
6. Monitor logs for any warnings or errors

---

## Support

If issues persist:
1. Check Railway logs: `railway logs`
2. Check build logs: `railway logs --deployment`
3. Verify all environment variables: `railway variables`
4. Test database connection: Railway dashboard → PostgreSQL → Connect
