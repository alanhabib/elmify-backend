# Database Setup Guide

## The Problem

You're getting this error repeatedly:
```
FATAL: database "elmify_db" does not exist
```

This happens because you're running PostgreSQL locally without Docker, and PostgreSQL doesn't auto-create databases.

## ✅ Best Practice Solution: Docker Compose

This is the **industry standard** used by companies like Spotify, Netflix, and major tech companies.

### Why Docker Compose?

- ✅ **Reproducible**: Same setup for all developers
- ✅ **Isolated**: No conflicts with system PostgreSQL
- ✅ **Automatic**: Database is created automatically
- ✅ **Complete**: Includes PostgreSQL + MinIO + networking
- ✅ **Clean**: Easy to reset/delete

### Setup (One-Time)

```bash
cd elmify-backend

# Start all services (PostgreSQL + MinIO)
docker-compose up -d

# Verify services are running
docker-compose ps
```

### Daily Workflow

```bash
# Start services
docker-compose up -d

# Start Spring Boot
./mvnw spring-boot:run

# When done, stop services (optional)
docker-compose down
```

### What Docker Compose Provides

```yaml
✅ PostgreSQL 15
   - Database: elmify_db (auto-created)
   - User: alanhabib
   - Password: password
   - Port: 5432

✅ MinIO (S3-compatible storage)
   - Bucket: elmify-audio (auto-created)
   - Port: 9000 (API)
   - Port: 9001 (Console)

✅ Automatic bucket creation
✅ Health checks
✅ Data persistence via volumes
```

## Alternative: Manual PostgreSQL

If you prefer using local PostgreSQL (not recommended):

```bash
# Create the database once
psql -U alanhabib -d postgres -c "CREATE DATABASE elmify_db;"

# Start your app
./mvnw spring-boot:run
```

## Why This Happens

PostgreSQL doesn't auto-create databases. The typical flow is:

1. ❌ **What's happening now:**
   ```
   Spring Boot starts → Tries to connect to elmify_db → Database doesn't exist → Crash
   ```

2. ✅ **What should happen:**
   ```
   Create database → Spring Boot starts → Flyway migrates schema → Success
   ```

## Long-Term Solution

### Add to your workflow:

1. **First time setup:**
   ```bash
   ./init-db.sh  # Creates database once
   ```

2. **Daily development:**
   ```bash
   ./start.sh    # Always checks database before starting
   ```

### Or create an alias:

Add to your `~/.zshrc` or `~/.bashrc`:
```bash
alias elmify-start='cd ~/Desktop/hobby_projects/elmify-backend && ./start.sh'
```

Then just run:
```bash
elmify-start
```

## Database Management Commands

### Check if database exists
```bash
psql -U alanhabib -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='elmify_db'"
```

### List all databases
```bash
psql -U alanhabib -l
```

### Drop database (if you need to reset)
```bash
psql -U alanhabib -d postgres -c "DROP DATABASE IF EXISTS elmify_db;"
```

### Recreate from scratch
```bash
# Drop old database
psql -U alanhabib -d postgres -c "DROP DATABASE IF EXISTS elmify_db;"

# Create fresh database
psql -U alanhabib -d postgres -c "CREATE DATABASE elmify_db;"

# Start Spring Boot (Flyway will create schema)
./mvnw spring-boot:run
```

## Flyway Migrations

Once the database exists, Flyway will:
1. Create the `flyway_schema_history` table
2. Run all migrations in `src/main/resources/db/migration`
3. Track which migrations have been applied

### Check migration status
```bash
./mvnw flyway:info
```

### Clean database (DANGEROUS - deletes all data)
```bash
./mvnw flyway:clean
./mvnw flyway:migrate
```

## Troubleshooting

### Error: "FATAL: database elmify_db does not exist"
**Solution:** Run `./init-db.sh` or create manually with psql

### Error: "psql: command not found"
**Solution:** Install PostgreSQL:
```bash
brew install postgresql@14
brew services start postgresql@14
```

### Error: "role alanhabib does not exist"
**Solution:** Create the PostgreSQL user:
```bash
createuser -s alanhabib
```

### Error: "Flyway migration failed"
**Solution:** Check migration files in `src/main/resources/db/migration/`

### Database exists but schema is wrong
**Solution:**
```bash
# Reset database
psql -U alanhabib -d postgres -c "DROP DATABASE elmify_db;"
psql -U alanhabib -d postgres -c "CREATE DATABASE elmify_db;"

# Restart app (Flyway will recreate schema)
./start.sh
```

## Production (Railway)

On Railway, the database is automatically created by the PostgreSQL service. You just need to set the `DATABASE_URL` environment variable.

Railway format:
```
DATABASE_URL=postgresql://user:password@host:port/database
```

Spring Boot will parse this automatically.

## Current Configuration

### Development (`application-dev.yml`)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/elmify_db
    username: alanhabib
    password: password
```

### Production (Railway)
Uses `DATABASE_URL` environment variable from Railway PostgreSQL service.
