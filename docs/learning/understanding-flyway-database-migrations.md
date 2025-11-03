# Understanding Flyway Database Migrations

## Table of Contents
1. [What is Flyway?](#what-is-flyway)
2. [Why Do We Need Database Migrations?](#why-do-we-need-database-migrations)
3. [How Flyway Works](#how-flyway-works)
4. [Migration File Naming Convention](#migration-file-naming-convention)
5. [Your Application's Migrations](#your-applications-migrations)
6. [Writing Migration Scripts](#writing-migration-scripts)
7. [Migration Best Practices](#migration-best-practices)
8. [Flyway Lifecycle](#flyway-lifecycle)
9. [Common Migration Patterns](#common-migration-patterns)
10. [Troubleshooting](#troubleshooting)

---

## What is Flyway?

**Flyway** is a **database version control and migration tool** that manages changes to your database schema over time. Think of it like Git for your database structure.

### The Problem Flyway Solves

Imagine you're working on your AudibleClone application:

**Without Flyway:**
```
Day 1: You manually create a `users` table in your local database
Day 5: You add a `favorites` table, but forget to document it
Day 10: Your teammate clones the project and their app crashes because
        they don't have the `favorites` table
Day 15: You deploy to production and can't remember which tables you created
```

**With Flyway:**
```
Day 1: You write V1__create_initial_schema.sql - Creates users table
Day 5: You write V8__create_favorites_table.sql - Flyway automatically
       applies this on all environments
Day 10: Your teammate runs the app - Flyway automatically creates all tables
Day 15: You deploy to production - Flyway applies only the missing migrations
```

### Key Benefits

1. **Version Control for Your Database** - Every schema change is tracked in Git
2. **Automatic Application** - Migrations run automatically when the application starts
3. **Environment Consistency** - Dev, staging, and prod databases stay in sync
4. **Rollback Safety** - You can see exactly what changed and when
5. **Team Collaboration** - Everyone gets the same database structure

---

## Why Do We Need Database Migrations?

### The Traditional Approach (Manual SQL)

Before tools like Flyway, developers would:
1. Manually run SQL scripts on their database
2. Email SQL files to teammates
3. Hope everyone ran the scripts in the correct order
4. Pray that production updates didn't get forgotten

This led to **database drift** - where different environments had different schemas.

### The Modern Approach (Flyway)

With Flyway:
1. Write a numbered SQL file (e.g., `V1__create_users.sql`)
2. Commit it to Git
3. Flyway automatically applies it to all environments
4. Flyway tracks which migrations have been applied using a special table

---

## How Flyway Works

### The `flyway_schema_history` Table

When Flyway runs for the first time, it creates a special table called `flyway_schema_history`:

```sql
SELECT * FROM flyway_schema_history;
```

| installed_rank | version | description              | type | script                           | checksum   | installed_on        | success |
|----------------|---------|--------------------------|------|----------------------------------|------------|---------------------|---------|
| 1              | 1       | create initial schema    | SQL  | V1__create_initial_schema.sql    | 1234567890 | 2025-01-15 10:30:00 | true    |
| 2              | 2       | insert core              | SQL  | V2__insert_core.sql              | 9876543210 | 2025-01-15 10:30:01 | true    |
| 3              | 3       | lectures data            | SQL  | V3__lectures_data.sql            | 5555555555 | 2025-01-15 10:30:02 | true    |
| 4              | 8       | create favorites table   | SQL  | V8__create_favorites_table.sql   | 1111111111 | 2025-01-20 14:15:00 | true    |

This table tracks:
- **version** - The migration version number
- **description** - Human-readable name (from filename)
- **checksum** - Hash of the file content (detects changes to applied migrations)
- **installed_on** - When the migration was applied
- **success** - Whether it succeeded

### The Migration Process

Here's what happens when your Spring Boot application starts:

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Spring Boot Application Starts                              │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. Flyway Checks for flyway_schema_history Table               │
│    - If missing, creates it                                     │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. Flyway Scans /db/migration/ for SQL Files                   │
│    - Finds: V1__*.sql, V2__*.sql, ..., V8__*.sql              │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. Flyway Compares with flyway_schema_history                  │
│    - V1, V2, V3 already applied ✓                              │
│    - V8 is new! Needs to be applied                            │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5. Flyway Applies Pending Migrations in Order                  │
│    - Runs V8__create_favorites_table.sql                       │
│    - Records it in flyway_schema_history                       │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 6. Application Continues Startup                               │
│    - Database is now up-to-date                                │
└─────────────────────────────────────────────────────────────────┘
```

---

## Migration File Naming Convention

Flyway is **very strict** about file naming. The format is:

```
V{VERSION}__{DESCRIPTION}.sql
```

### Breaking It Down

- **`V`** - Prefix indicating this is a versioned migration (MUST be uppercase)
- **`{VERSION}`** - Version number (e.g., `1`, `2`, `3`, `7`, `8`)
- **`__`** - **TWO underscores** (separator between version and description)
- **`{DESCRIPTION}`** - Human-readable description using underscores (e.g., `create_users_table`)
- **`.sql`** - File extension

### ✅ Valid Examples

```
V1__create_initial_schema.sql          ← Version 1
V2__insert_core.sql                    ← Version 2
V3__lectures_data.sql                  ← Version 3
V7__add_user_refine_relationships.sql  ← Version 7
V8__create_favorites_table.sql         ← Version 8
V10__add_username_column.sql           ← Version 10 (two digits)
V2_1__hotfix_users.sql                 ← Version 2.1 (dot versioning)
```

### ❌ Invalid Examples

```
v1__create_users.sql                   ← Lowercase 'v' (WRONG!)
V1_create_users.sql                    ← Only one underscore (WRONG!)
V1__create users.sql                   ← Space in description (WRONG!)
create_users.sql                       ← No version prefix (WRONG!)
V1.5__update_users.sql                 ← Dot not supported this way (use V1_5)
```

### Version Number Rules

1. **Sequential but can have gaps**: V1, V2, V3, V8 is fine (V4-V7 don't need to exist)
2. **Cannot apply lower versions after higher ones**: If V8 is applied, you can't add V4
3. **Can use dot notation**: V1.1, V1.2, V2.0 for hotfixes
4. **Must be unique**: Two migrations can't have the same version number

---

## Your Application's Migrations

Let's walk through your actual migrations in chronological order:

### Migration Timeline

```
V1__create_initial_schema.sql       (Day 1)
  ↓
V2__insert_core.sql                 (Day 1 - after schema)
  ↓
V3__lectures_data.sql               (Day 2 - seed data)
  ↓
V4__analytics_data.sql              (Day 3 - analytics setup)
  ↓
V5__creation_indexes.sql            (Day 4 - performance)
  ↓
V6__user_saved_lectures.sql         (Day 5 - user features)
  ↓
V7__add_user_refine_relationships.sql (Day 10 - major refactor)
  ↓
V8__create_favorites_table.sql      (Day 15 - favorites feature)
```

---

### V1: Initial Schema (Foundation)

**File:** `V1__create_initial_schema.sql`

**Purpose:** Creates the core database structure for the entire application.

**What It Creates:**

```sql
-- Core Content Tables
speakers               -- Who creates the lectures
collections            -- Grouped lectures (albums, series)
lectures               -- Individual audio files

-- User Interaction Tables
user_saved_lectures    -- Bookmarked lectures
playback_positions     -- Where user left off in each lecture
listening_stats        -- Analytics per user per lecture per day

-- User Goals Tables
user_daily_goals       -- Daily listening goals and streaks
lecture_play_credits   -- Track which lectures count toward goals

-- Views
daily_progress         -- Aggregated daily listening stats
```

**Key Design Decisions:**

1. **Referential Integrity**: Uses foreign keys with `ON DELETE CASCADE`
   ```sql
   collection_id INTEGER REFERENCES collections(id) ON DELETE CASCADE
   ```
   This means: "If a collection is deleted, delete all its lectures automatically"

2. **Timestamps Everywhere**: Most tables have `created_at` and `updated_at`
   ```sql
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
   ```

3. **Auto-Update Trigger**: Updates `updated_at` automatically
   ```sql
   CREATE OR REPLACE FUNCTION update_timestamp_function()
   RETURNS TRIGGER AS $$
   BEGIN
      NEW.updated_at = NOW();
      RETURN NEW;
   END;
   $$ language 'plpgsql';
   ```

4. **User IDs as TEXT**: Initially used Clerk IDs directly (changed later in V7)

---

### V7: User Refactoring (Major Change)

**File:** `V7__add_user_refine_relationships.sql`

**Purpose:** Introduced a proper `users` table and converted user_id from TEXT (Clerk ID) to INTEGER (database ID).

**Why This Was Needed:**

**Before V7 (Problems):**
```sql
-- Every table stored Clerk IDs directly
playback_positions (
  user_id TEXT,  -- "user_30sZ4mdRbYN9ALg2z7jy9lVs2PM"
  ...
)

favorites (
  user_id TEXT,  -- "user_30sZ4mdRbYN9ALg2z7jy9lVs2PM"
  ...
)
```

Issues:
- ❌ No user profile information stored
- ❌ Can't query user email or name
- ❌ Clerk ID stored redundantly in every row (wasted space)
- ❌ No referential integrity (can't use foreign keys to a non-existent table)

**After V7 (Fixed):**
```sql
-- New users table
users (
  id SERIAL PRIMARY KEY,          -- Database ID: 1, 2, 3
  clerk_id TEXT UNIQUE,           -- "user_30sZ4mdRbYN9ALg2z7jy9lVs2PM"
  email TEXT,
  display_name TEXT,
  profile_image_url TEXT,
  is_premium BOOLEAN
)

-- Other tables now reference users by ID
playback_positions (
  user_id INTEGER REFERENCES users(id),  -- 1, 2, 3
  ...
)
```

Benefits:
- ✅ Stores complete user profile
- ✅ Efficient integer foreign keys
- ✅ Referential integrity with `ON DELETE CASCADE`
- ✅ Can join to get user details

**Safe Data Migration Strategy:**

The migration was designed to handle existing data safely:

```sql
-- Step 1: Check if data exists
DO $$
DECLARE
  playback_count INTEGER;
BEGIN
  SELECT COUNT(*) INTO playback_count FROM playback_positions;

  IF playback_count > 0 THEN
    RAISE NOTICE 'Existing data found. Clearing to allow migration...';
    DELETE FROM playback_positions;  -- Clear old data
  END IF;
END $$;

-- Step 2: Drop the old TEXT column
ALTER TABLE playback_positions DROP COLUMN user_id;

-- Step 3: Add new INTEGER column with foreign key
ALTER TABLE playback_positions ADD COLUMN user_id INTEGER NOT NULL
  REFERENCES users(id) ON DELETE CASCADE;
```

**Production Note:** In a real production environment with existing users, you'd:
1. Create a mapping table (clerk_id → user_id)
2. Migrate data using that mapping
3. Then drop the old column

For development (with no real users yet), deleting the data was fine.

**Schema Cleanup:**

Also removed redundant columns from `lectures` table:
```sql
-- These were redundant because we have foreign keys
ALTER TABLE lectures DROP COLUMN speaker;      -- Use speaker_id instead
ALTER TABLE lectures DROP COLUMN collection;   -- Use collection_id instead
ALTER TABLE lectures DROP COLUMN user_id;      -- You're the only uploader
```

---

### V8: Favorites Table (Latest Feature)

**File:** `V8__create_favorites_table.sql`

**Purpose:** Allow users to favorite/save lectures for quick access.

**Complete Migration:**

```sql
CREATE TABLE IF NOT EXISTS favorites (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    lecture_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key to users table
    CONSTRAINT fk_favorites_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    -- Foreign key to lectures table
    CONSTRAINT fk_favorites_lecture
        FOREIGN KEY (lecture_id)
        REFERENCES lectures(id)
        ON DELETE CASCADE,

    -- Prevent duplicate favorites
    CONSTRAINT uq_user_lecture
        UNIQUE (user_id, lecture_id)
);

-- Performance indexes
CREATE INDEX idx_favorites_user_id ON favorites(user_id);
CREATE INDEX idx_favorites_lecture_id ON favorites(lecture_id);
CREATE INDEX idx_favorites_created_at ON favorites(created_at DESC);
```

**Key Design Patterns:**

1. **Composite Unique Constraint**
   ```sql
   CONSTRAINT uq_user_lecture UNIQUE (user_id, lecture_id)
   ```
   Prevents a user from favoriting the same lecture twice.

2. **Cascade Deletions**
   ```sql
   ON DELETE CASCADE
   ```
   If user or lecture is deleted, the favorite record is automatically removed.

3. **Strategic Indexes**
   ```sql
   CREATE INDEX idx_favorites_user_id ON favorites(user_id);
   ```
   Makes queries like "get all favorites for user 1" very fast.

   ```sql
   CREATE INDEX idx_favorites_created_at ON favorites(created_at DESC);
   ```
   Makes "recent favorites" queries fast (sorted by newest first).

4. **Documentation Comments**
   ```sql
   COMMENT ON TABLE favorites IS 'User favorite lectures';
   COMMENT ON COLUMN favorites.user_id IS 'Reference to user';
   ```
   PostgreSQL stores these comments and tools can display them.

---

## Writing Migration Scripts

### General Structure

Every migration should follow this structure:

```sql
-- ============================================================
-- Migration: V{VERSION}__{DESCRIPTION}
-- Purpose: Brief description of what this migration does
-- ============================================================

-- Optional: Set PostgreSQL settings
SET client_encoding = 'UTF8';

-- Optional: Use transactions for safety
BEGIN;

-- === Main Changes ===

-- 1. Create new tables
CREATE TABLE my_new_table (
  id SERIAL PRIMARY KEY,
  name TEXT NOT NULL
);

-- 2. Alter existing tables
ALTER TABLE existing_table ADD COLUMN new_column TEXT;

-- 3. Create indexes
CREATE INDEX idx_my_new_table_name ON my_new_table(name);

-- 4. Insert seed data (if needed)
INSERT INTO my_new_table (name) VALUES ('Example');

-- Optional: Commit transaction
COMMIT;
```

### Example: Adding a New Column

**Migration:** `V9__add_lecture_rating.sql`

```sql
-- Add rating system to lectures
BEGIN;

-- Add the column with a default value
ALTER TABLE lectures
ADD COLUMN average_rating DECIMAL(3,2) DEFAULT 0.00;

-- Add a constraint to ensure rating is between 0 and 5
ALTER TABLE lectures
ADD CONSTRAINT check_rating_range
CHECK (average_rating >= 0.00 AND average_rating <= 5.00);

-- Add index for queries that filter by rating
CREATE INDEX idx_lectures_rating ON lectures(average_rating DESC);

-- Add comment
COMMENT ON COLUMN lectures.average_rating IS 'Average user rating (0.00 to 5.00)';

COMMIT;
```

### Example: Creating a New Table with Relationships

**Migration:** `V10__create_reviews_table.sql`

```sql
-- Create reviews table for user lecture reviews
BEGIN;

CREATE TABLE IF NOT EXISTS reviews (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  lecture_id BIGINT NOT NULL,
  rating INTEGER NOT NULL,
  comment TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  -- Foreign keys
  CONSTRAINT fk_reviews_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE,

  CONSTRAINT fk_reviews_lecture
    FOREIGN KEY (lecture_id)
    REFERENCES lectures(id)
    ON DELETE CASCADE,

  -- Constraints
  CONSTRAINT uq_user_lecture_review
    UNIQUE (user_id, lecture_id),

  CONSTRAINT check_rating_value
    CHECK (rating >= 1 AND rating <= 5)
);

-- Indexes for common queries
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
CREATE INDEX idx_reviews_lecture_id ON reviews(lecture_id);
CREATE INDEX idx_reviews_rating ON reviews(rating);
CREATE INDEX idx_reviews_created_at ON reviews(created_at DESC);

-- Trigger for updated_at
CREATE TRIGGER trigger_reviews_updated_at
BEFORE UPDATE ON reviews
FOR EACH ROW
EXECUTE FUNCTION update_timestamp_function();

COMMIT;
```

---

## Migration Best Practices

### 1. **Never Modify Applied Migrations**

Once a migration has been applied (exists in `flyway_schema_history`), **NEVER edit it**.

❌ **WRONG:**
```sql
-- V8__create_favorites_table.sql (already applied)
CREATE TABLE favorites (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  lecture_id BIGINT NOT NULL
  -- Oops, forgot created_at! Let me add it...
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP  -- DON'T DO THIS!
);
```

✅ **CORRECT:**
```sql
-- Create a new migration instead
-- V9__add_created_at_to_favorites.sql
ALTER TABLE favorites
ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
```

**Why?** Flyway stores a checksum of each migration. If you modify it, Flyway will detect the change and **fail on startup** with:
```
Migration checksum mismatch for migration version 8
```

### 2. **Use Transactions for Safety**

Wrap your migrations in transactions so they're **atomic** (all-or-nothing):

```sql
BEGIN;

  CREATE TABLE users (...);
  CREATE INDEX idx_users_email ON users(email);
  ALTER TABLE lectures ADD COLUMN user_id INTEGER;

COMMIT;
```

If any statement fails, the entire migration is rolled back.

### 3. **Make Migrations Idempotent (When Possible)**

Use `IF NOT EXISTS` / `IF EXISTS` to make migrations re-runnable:

```sql
-- Safe to run multiple times
CREATE TABLE IF NOT EXISTS users (...);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Conditional logic
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'users' AND column_name = 'email'
  ) THEN
    ALTER TABLE users ADD COLUMN email TEXT;
  END IF;
END $$;
```

### 4. **Add Comments and Documentation**

```sql
-- Table comments
COMMENT ON TABLE users IS 'Application users synced from Clerk';

-- Column comments
COMMENT ON COLUMN users.clerk_id IS 'Unique Clerk authentication ID';
COMMENT ON COLUMN users.is_premium IS 'Whether user has premium subscription';
```

These appear in database tools (DBeaver, pgAdmin) and help future developers.

### 5. **Use Descriptive Names**

❌ **BAD:**
```
V9__update.sql
V10__fix.sql
V11__changes.sql
```

✅ **GOOD:**
```
V9__add_lecture_rating_column.sql
V10__create_reviews_table.sql
V11__add_indexes_for_search.sql
```

### 6. **Separate Data and Schema Migrations**

**Schema migrations** (structure):
```sql
-- V9__add_premium_features_schema.sql
CREATE TABLE premium_content (...);
ALTER TABLE users ADD COLUMN is_premium BOOLEAN DEFAULT FALSE;
```

**Data migrations** (content):
```sql
-- V10__seed_premium_content.sql
INSERT INTO premium_content (lecture_id, tier) VALUES (1, 'gold');
UPDATE users SET is_premium = TRUE WHERE email = 'admin@example.com';
```

This makes it easier to understand what each migration does.

### 7. **Handle Existing Data Carefully**

When altering tables with data, consider existing rows:

```sql
-- Adding a NOT NULL column requires a default
ALTER TABLE lectures
ADD COLUMN visibility TEXT NOT NULL DEFAULT 'public';

-- Or use a two-step process
ALTER TABLE lectures ADD COLUMN visibility TEXT;  -- Nullable first
UPDATE lectures SET visibility = 'public';        -- Set values
ALTER TABLE lectures ALTER COLUMN visibility SET NOT NULL;  -- Then enforce
```

### 8. **Test Migrations in Development First**

Before committing a migration:

1. **Test on a clean database** (no existing data)
2. **Test on a database with seed data**
3. **Test rollback scenario** (if you support rollbacks)

---

## Flyway Lifecycle

### Startup Sequence

```
Spring Boot Starts
       ↓
Flyway AutoConfiguration Triggered
       ↓
┌─────────────────────────────────┐
│ Connect to Database             │
└─────────────────────────────────┘
       ↓
┌─────────────────────────────────┐
│ Check flyway_schema_history     │
│ - Create if missing             │
└─────────────────────────────────┘
       ↓
┌─────────────────────────────────┐
│ Scan classpath:db/migration/    │
│ - Find all V*.sql files         │
└─────────────────────────────────┘
       ↓
┌─────────────────────────────────┐
│ Compare with Applied Migrations │
│ - Which are pending?            │
└─────────────────────────────────┘
       ↓
┌─────────────────────────────────┐
│ Sort Pending Migrations         │
│ - By version number (ascending) │
└─────────────────────────────────┘
       ↓
┌─────────────────────────────────┐
│ Execute Each Migration          │
│ - Run SQL                       │
│ - Record in schema_history      │
│ - Calculate checksum            │
└─────────────────────────────────┘
       ↓
┌─────────────────────────────────┐
│ Verify Checksums                │
│ - Detect modified migrations    │
└─────────────────────────────────┘
       ↓
JPA/Hibernate Starts (entities can now use the schema)
       ↓
Application Ready
```

### Configuration in Spring Boot

In your `application.yml`:

```yaml
spring:
  flyway:
    enabled: true                          # Enable Flyway (default: true)
    locations: classpath:db/migration      # Where to find migrations
    baseline-on-migrate: true              # Allow migrations on existing DBs
    validate-on-migrate: true              # Check checksums on startup
```

You can also disable Flyway for certain profiles:

```yaml
# application-test.yml
spring:
  flyway:
    enabled: false  # Don't run migrations in tests (use in-memory DB instead)
```

---

## Common Migration Patterns

### Pattern 1: Adding a Column

```sql
-- V11__add_lecture_description.sql
BEGIN;

ALTER TABLE lectures
ADD COLUMN description TEXT;

COMMENT ON COLUMN lectures.description
IS 'Detailed description of the lecture content';

COMMIT;
```

### Pattern 2: Renaming a Column

```sql
-- V12__rename_title_to_display_title.sql
BEGIN;

ALTER TABLE lectures
RENAME COLUMN title TO display_title;

COMMIT;
```

### Pattern 3: Adding an Index

```sql
-- V13__add_search_indexes.sql
BEGIN;

-- For full-text search
CREATE INDEX idx_lectures_title_search
ON lectures USING gin(to_tsvector('english', title));

-- For sorting
CREATE INDEX idx_lectures_created_at
ON lectures(created_at DESC);

COMMIT;
```

### Pattern 4: Creating a Join Table (Many-to-Many)

```sql
-- V14__create_lecture_tags.sql
BEGIN;

-- Tag table
CREATE TABLE tags (
  id SERIAL PRIMARY KEY,
  name TEXT NOT NULL UNIQUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Join table
CREATE TABLE lecture_tags (
  lecture_id BIGINT NOT NULL REFERENCES lectures(id) ON DELETE CASCADE,
  tag_id INTEGER NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
  PRIMARY KEY (lecture_id, tag_id)
);

CREATE INDEX idx_lecture_tags_lecture_id ON lecture_tags(lecture_id);
CREATE INDEX idx_lecture_tags_tag_id ON lecture_tags(tag_id);

COMMIT;
```

### Pattern 5: Data Transformation

```sql
-- V15__normalize_speaker_names.sql
BEGIN;

-- Convert all speaker names to title case
UPDATE speakers
SET name = INITCAP(name);

COMMIT;
```

### Pattern 6: Adding a Constraint

```sql
-- V16__add_email_format_check.sql
BEGIN;

ALTER TABLE users
ADD CONSTRAINT check_email_format
CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

COMMIT;
```

### Pattern 7: Creating a View

```sql
-- V17__create_popular_lectures_view.sql
BEGIN;

CREATE OR REPLACE VIEW popular_lectures AS
SELECT
  l.id,
  l.title,
  s.name AS speaker_name,
  l.play_count,
  COUNT(f.id) AS favorite_count
FROM lectures l
LEFT JOIN speakers s ON l.speaker_id = s.id
LEFT JOIN favorites f ON l.id = f.lecture_id
GROUP BY l.id, l.title, s.name, l.play_count
HAVING l.play_count > 100 OR COUNT(f.id) > 10
ORDER BY l.play_count DESC, favorite_count DESC;

COMMIT;
```

---

## Troubleshooting

### Problem 1: "Migration Checksum Mismatch"

**Error:**
```
Migration checksum mismatch for migration version 8
Applied to database: 1234567890
Resolved locally:   9876543210
```

**Cause:** You edited an already-applied migration file.

**Solution:**

**Option A - Development Only (Clean Database):**
```sql
-- WARNING: This deletes all data!
DROP DATABASE audibleclone_dev;
CREATE DATABASE audibleclone_dev;
-- Restart app - migrations run from scratch
```

**Option B - Production (Repair):**
```bash
# Update the checksum in flyway_schema_history to match the file
./mvnw flyway:repair
```

**Best Practice:** Never edit applied migrations. Create a new migration instead.

---

### Problem 2: "Migration Failed"

**Error:**
```
Migration V9__add_rating.sql failed
SQL State: 42P07
Message: relation "rating" already exists
```

**Cause:** Migration tried to create something that already exists, or there was an error in the SQL.

**Solution:**

1. **Fix the migration file**
2. **Clean up the failed migration from the database:**
   ```sql
   DELETE FROM flyway_schema_history WHERE version = '9';
   -- Also manually undo any partial changes the migration made
   ```
3. **Restart the application**

**Prevention:** Use `IF NOT EXISTS` clauses and test migrations thoroughly.

---

### Problem 3: "Migrations Out of Order"

**Error:**
```
Detected resolved migration not applied to database: 5
But migration 8 has already been applied
```

**Cause:** You added V5, but V8 was already applied.

**Solution:**

You have two options:

**Option A - Rename to higher version:**
```bash
# Rename the file
mv V5__new_feature.sql V9__new_feature.sql
```

**Option B - Allow out-of-order (not recommended):**
```yaml
# application.yml
spring:
  flyway:
    out-of-order: true
```

**Best Practice:** Always use the next available version number.

---

### Problem 4: "Flyway Locked"

**Error:**
```
Unable to acquire lock for migration
```

**Cause:** Multiple application instances trying to migrate simultaneously, or a previous migration crashed.

**Solution:**

```sql
-- Check the lock
SELECT * FROM flyway_schema_history WHERE version = '0';

-- Release the lock (if you're sure no migration is running)
DELETE FROM flyway_schema_history WHERE version = '0';
```

---

### Problem 5: "File Not Found"

**Error:**
```
Unable to read migration file: V8__create_favorites_table.sql
```

**Cause:** File not in the correct location.

**Solution:**

Ensure file is in: `src/main/resources/db/migration/`

```
src/
└── main/
    └── resources/
        └── db/
            └── migration/
                ├── V1__create_initial_schema.sql
                ├── V2__insert_core.sql
                └── V8__create_favorites_table.sql
```

---

## Key Takeaways

### What You Learned

1. **Flyway is Version Control for Your Database**
   - Tracks every schema change
   - Applies migrations automatically
   - Ensures consistency across environments

2. **Migration Naming is Strict**
   - Format: `V{VERSION}__{DESCRIPTION}.sql`
   - Must have uppercase V, two underscores, and .sql extension

3. **Your Migration History**
   - V1: Created initial schema (speakers, collections, lectures, etc.)
   - V7: Major refactor - added users table, changed user_id from TEXT to INTEGER
   - V8: Added favorites feature with proper foreign keys and indexes

4. **Best Practices**
   - ✅ Never modify applied migrations
   - ✅ Use transactions (BEGIN/COMMIT)
   - ✅ Make migrations idempotent with IF NOT EXISTS
   - ✅ Add comments and documentation
   - ✅ Test migrations before committing

5. **Common Patterns**
   - Adding columns, indexes, constraints
   - Creating join tables for many-to-many relationships
   - Data transformations
   - Creating views

### How This Connects to Your Application

```
Migration Files (db/migration/V*.sql)
              ↓
         Flyway Applies
              ↓
     Database Schema Created
              ↓
    JPA Entities Map to Tables
              ↓
  Spring Data Repositories Query Tables
              ↓
         Services Use Repositories
              ↓
      Controllers Expose APIs
              ↓
    React Native App Consumes APIs
```

### Next Steps

Now that you understand Flyway:

1. **When to Create a Migration:**
   - Any time you add/modify a JPA entity
   - Any time you need to change the database structure
   - Any time you need to seed or transform data

2. **How to Create a Migration:**
   - Create `V{NEXT_VERSION}__descriptive_name.sql`
   - Write the SQL (use BEGIN/COMMIT)
   - Test locally
   - Commit to Git
   - Flyway applies automatically on all environments

3. **Related Guides:**
   - [Understanding JPA and Hibernate](./understanding-jpa-and-hibernate.md) - How entities map to these tables
   - [Understanding @ManyToOne Relationships](./understanding-manytoone-relationships.md) - How foreign keys work

---

**Congratulations!** You now understand how database migrations work in your AudibleClone application. Every time you modify the database schema, you'll create a Flyway migration to ensure all environments stay in sync.
