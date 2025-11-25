# Deployment Troubleshooting - Healthcheck Timeout

## Current Issue
Build succeeds but healthcheck times out after 5 minutes.

---

## Quick Diagnosis Steps

### Step 1: Check Railway Logs (MOST IMPORTANT)

```bash
cd elmify-backend
railway logs
```

**What to look for:**

✅ **Success - Application started:**
```
Started ElmifyApplication in 15.234 seconds
Tomcat started on port(s): 8080 (http)
```

❌ **Database Connection Error:**
```
Failed to configure a DataSource
Cannot create PoolableConnectionFactory
Connection refused
```
**Fix:** Check DATABASE_URL, DB_USERNAME, DB_PASSWORD are set correctly

❌ **Clerk Configuration Error:**
```
Failed to configure Clerk JWT decoder
Invalid CLERK_SECRET_KEY
```
**Fix:** Verify CLERK_SECRET_KEY, CLERK_JWT_ISSUER are correct

❌ **R2 Configuration Error:**
```
Failed to initialize R2 client
Invalid credentials
```
**Fix:** Verify R2_ENDPOINT, R2_ACCESS_KEY, R2_SECRET_KEY

❌ **Flyway Migration Error:**
```
FlywayException: Found non-empty schema(s)
Migration checksum mismatch
```
**Fix:** Set `spring.flyway.baseline-on-migrate=true` (already done)

❌ **Port Binding Error:**
```
Port 8080 is already in use
Failed to bind to 0.0.0.0:8080
```
**Fix:** Ensure PORT variable is set correctly

❌ **Out of Memory:**
```
java.lang.OutOfMemoryError: Java heap space
```
**Fix:** Increase memory in railway.json startCommand

---

## Step 2: Enable Debug Logging

### Option A: Temporarily use debug startup script

1. Rename `railway.json` to `railway-original.json`
2. Rename `railway-debug.json` to `railway.json`
3. Deploy: `railway up`
4. Check logs: `railway logs`

This will show:
- All environment variables (masked)
- Java version
- Missing variables
- JAR file status
- Detailed startup logs

### Option B: Add debug flag to railway.json

Edit `railway.json` startCommand:
```json
"startCommand": "java -Xmx400m -Xms200m -XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=prod -Dlogging.level.root=DEBUG -jar app.jar"
```

---

## Step 3: Verify Environment Variables

### Check what's actually set in Railway:

```bash
railway variables
```

### Required variables checklist:

- [ ] `DATABASE_URL` - starts with `jdbc:postgresql://`
- [ ] `DB_USERNAME` - database username
- [ ] `DB_PASSWORD` - database password
- [ ] `CLERK_SECRET_KEY` - starts with `sk_live_` or `sk_test_`
- [ ] `CLERK_PUBLISHABLE_KEY` - starts with `pk_live_` or `pk_test_`
- [ ] `CLERK_JWT_ISSUER` - URL like `https://your-app.clerk.accounts.dev`
- [ ] `R2_ENDPOINT` - Cloudflare R2 endpoint
- [ ] `R2_BUCKET_NAME` - bucket name
- [ ] `R2_ACCESS_KEY` - R2 access key
- [ ] `R2_SECRET_KEY` - R2 secret key
- [ ] `PORT` - should be `8080`
- [ ] `SPRING_PROFILES_ACTIVE` - should be `prod`

### Common mistakes:

❌ **Wrong DATABASE_URL format:**
```bash
# WRONG (missing jdbc:)
DATABASE_URL=postgresql://host:5432/db

# CORRECT
DATABASE_URL=jdbc:postgresql://host:5432/db
```

❌ **Using test keys in production:**
```bash
# WRONG (test keys)
CLERK_SECRET_KEY=sk_test_...

# CORRECT (production keys)
CLERK_SECRET_KEY=sk_live_...
```

❌ **Wrong Clerk issuer:**
```bash
# WRONG (generic)
CLERK_JWT_ISSUER=https://clerk.com

# CORRECT (your specific instance)
CLERK_JWT_ISSUER=https://your-app-name.clerk.accounts.dev
```

---

## Step 4: Test Database Connection

### Check if PostgreSQL is running:

In Railway dashboard:
1. Go to your project
2. Click on PostgreSQL service
3. Check status is "Active"
4. Check "Connect" tab for connection details

### Verify DATABASE_URL transformation:

Railway gives you: `postgres://user:pass@host:5432/railway`

You need to set:
```bash
DATABASE_URL=jdbc:postgresql://host:5432/railway
DB_USERNAME=user
DB_PASSWORD=pass
```

**Or use Railway variable references:**
```bash
DATABASE_URL=jdbc:postgresql://${{Postgres.POSTGRES_HOST}}:${{Postgres.POSTGRES_PORT}}/${{Postgres.POSTGRES_DB}}
DB_USERNAME=${{Postgres.POSTGRES_USER}}
DB_PASSWORD=${{Postgres.POSTGRES_PASSWORD}}
```

---

## Step 5: Common Issues & Solutions

### Issue: "Application started but healthcheck still fails"

**Cause:** Healthcheck path might be secured by authentication

**Check:** Is `/actuator/health` accessible without authentication?

**Fix:** Verify in `SecurityConfig.java`:
```java
.requestMatchers("/actuator/health/**").permitAll()
```

### Issue: "Connection pool error"

**Symptoms:**
```
HikariPool-1 - Exception during pool initialization
Connection is not available
```

**Causes:**
1. Database not accessible from Railway
2. Wrong credentials
3. Database not created yet

**Fix:**
1. Check PostgreSQL is running: Railway dashboard → PostgreSQL → Status
2. Verify credentials match what Railway provides
3. Check if database exists: `railway connect postgres` then `\l`

### Issue: "Flyway baseline error"

**Symptoms:**
```
Found non-empty schema(s) "public" but no schema history table
```

**Fix:** Already added to `application-prod.yml`:
```yaml
spring:
  flyway:
    baseline-on-migrate: true
    baseline-version: 0
```

If still failing, manually baseline:
```bash
railway connect postgres
# Then in psql:
CREATE TABLE IF NOT EXISTS flyway_schema_history (...);
```

### Issue: "Out of memory during startup"

**Symptoms:**
```
java.lang.OutOfMemoryError: Java heap space
GC overhead limit exceeded
```

**Fix:** Increase heap in `railway.json`:
```json
"startCommand": "java -Xmx512m -Xms256m ... -jar app.jar"
```

Or reduce startup memory usage by disabling dev-only features.

---

## Step 6: Nuclear Option - Fresh Database

If all else fails and you suspect database corruption:

**⚠️ WARNING: This deletes all data!**

```bash
# In Railway dashboard
1. Remove PostgreSQL service
2. Add new PostgreSQL service
3. Update DATABASE_URL variables
4. Redeploy
```

Flyway will create a fresh schema from migrations.

---

## Step 7: Local Testing

Test the production profile locally to isolate Railway-specific issues:

```bash
# Set all environment variables
export DATABASE_URL="jdbc:postgresql://localhost:5432/elmify_db"
export DB_USERNAME="alanhabib"
export DB_PASSWORD="password"
export CLERK_SECRET_KEY="sk_test_..."
export CLERK_PUBLISHABLE_KEY="pk_test_..."
export CLERK_JWT_ISSUER="https://your-app.clerk.accounts.dev"
export R2_ENDPOINT="your-r2-endpoint"
export R2_BUCKET_NAME="your-bucket"
export R2_ACCESS_KEY="your-key"
export R2_SECRET_KEY="your-secret"
export PORT="8080"
export SPRING_PROFILES_ACTIVE="prod"

# Run locally
./mvnw clean install
java -jar target/elmify-backend-1.0.0.jar

# Test healthcheck
curl http://localhost:8080/actuator/health
```

If it works locally but not on Railway → Railway configuration issue
If it fails locally → Application configuration issue

---

## Step 8: Get Help

If still stuck, collect this info:

1. **Railway logs** (last 100 lines):
   ```bash
   railway logs > railway-logs.txt
   ```

2. **Environment variables** (redact sensitive values):
   ```bash
   railway variables > railway-vars.txt
   ```

3. **Build logs**:
   Copy from Railway dashboard → Deployments → Latest → Build logs

4. **Database status**:
   Railway dashboard → PostgreSQL → Metrics (screenshot)

5. **Local test results**:
   Does it work locally with prod profile?

---

## Expected Successful Startup Logs

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.0)

2024-11-25T10:15:30.123Z  INFO 1 --- [           main] c.e.backend.ElmifyApplication            : Starting ElmifyApplication v1.0.0
2024-11-25T10:15:30.125Z  INFO 1 --- [           main] c.e.backend.ElmifyApplication            : The following 1 profile is active: "prod"
2024-11-25T10:15:32.456Z  INFO 1 --- [           main] o.s.d.r.c.LettuceConnectionFactory       : Initialized Lettuce client (or warning if Redis not configured)
2024-11-25T10:15:33.789Z  INFO 1 --- [           main] o.f.c.i.c.DbMigrate                      : Successfully applied X migration(s)
2024-11-25T10:15:34.012Z  INFO 1 --- [           main] c.e.backend.service.PlaylistManifest     : ✅ Redis caching enabled (or ⚠️ warning if disabled)
2024-11-25T10:15:35.234Z  INFO 1 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http)
2024-11-25T10:15:35.345Z  INFO 1 --- [           main] c.e.backend.ElmifyApplication            : Started ElmifyApplication in 15.234 seconds (process running for 16.123)
```

**Key indicators:**
- ✅ `Started ElmifyApplication in X seconds`
- ✅ `Tomcat started on port(s): 8080`
- ✅ `Successfully applied X migration(s)`
- ✅ No ERROR messages

Then healthcheck should pass immediately.
