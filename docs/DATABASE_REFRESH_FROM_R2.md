# Database Refresh from R2 - Step by Step Guide

This guide explains how to completely refresh your production database by importing all data directly from your Cloudflare R2 storage.

## Overview

The process uses an automated Node.js script that:
1. Scans your R2 bucket for all audio files, metadata, and images
2. Generates Flyway SQL migration files
3. Deploys to Railway where Flyway automatically runs the migrations

## Prerequisites

- Node.js installed locally
- R2 credentials configured
- Java 21 installed (for building the backend)
- Git access to push to Railway

## Step-by-Step Process

### Step 1: Navigate to Scripts Directory

```bash
cd /path/to/elmify-backend/scripts
```

### Step 2: Verify R2 Credentials

Ensure your `.env` file exists with the correct credentials:

```bash
cat .env
```

Should contain:
```env
R2_ENDPOINT=https://[your-account-id].r2.cloudflarestorage.com
R2_ACCESS_KEY_ID=your_access_key
R2_SECRET_ACCESS_KEY=your_secret_key
R2_BUCKET_NAME=elmify-audio
```

### Step 3: Run the R2 Import Script

```bash
node generate-migration-from-r2.js
```

**Expected Output:**
```
🚀 Starting R2 to PostgreSQL migration generator

📦 Scanning R2 bucket: elmify-audio...
  Found 532 objects so far...
✅ Total: 532 objects in R2

📁 Parsing folder structure...
✅ Found 8 speakers

📋 Loading collection metadata...
  ✓ Abdul Rashid Sufi / Quran Hafs
  ✓ Abdulrahman Hassan / Seerah of Prophet Muhammad ﷺ
  [... more collections ...]

🔨 Generating SQL...
  ✓ 8 speakers
  ✓ 36 collections
  ✓ 467 lectures

✅ Generated migration files in:
   /path/to/src/main/resources/db/migration
   - V24: Clean database
   - V25: Insert 8 speakers
   - V26: Insert 36 collections
   - V27-V31: Insert 467 lectures (5 parts)
```

### Step 4: Review Generated Migration Files

Check that the migrations were created:

```bash
ls -lh ../src/main/resources/db/migration/V{24..31}*.sql
```

You should see 8 files:
- `V24__clean_database_for_r2_import.sql` - Clears existing data
- `V25__insert_speakers_from_r2.sql` - Inserts speakers with images
- `V26__insert_collections_from_r2.sql` - Inserts collections with cover images
- `V27-V31__insert_lectures_from_r2_part1-5.sql` - Inserts all lectures

### Step 5: Build the Backend

```bash
cd ..
JAVA_HOME=/path/to/java21 ./mvnw clean package -DskipTests
```

**On macOS with Corretto 21:**
```bash
JAVA_HOME=/Users/alanhabib/Library/Java/JavaVirtualMachines/corretto-21.0.3/Contents/Home ./mvnw clean package -DskipTests
```

**Expected Output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: 3.738 s
```

### Step 6: Commit and Push

```bash
git add -A
git commit -m "Database refresh: Import all data from R2"
git push
```

### Step 7: Monitor Railway Deployment

1. Go to your Railway dashboard
2. Watch the deployment logs
3. Wait for migrations to complete (may take 3-5 minutes)

**What Happens During Deployment:**
1. Flyway detects new migration files (V24-V31)
2. **V24 runs first**: Deletes all existing data (speakers, collections, lectures, user data)
3. **V25 runs**: Inserts 8 speakers with CDN image URLs
4. **V26 runs**: Inserts 36 collections with CDN cover image URLs
5. **V27-V31 run**: Inserts 467 lectures in 5 batches
6. Backend starts serving the new data

### Step 8: Verify Deployment Success

Check the deployment logs for:
```
✅ Migration to version 24 successful
✅ Migration to version 25 successful
✅ Migration to version 26 successful
✅ Migration to version 27 successful
...
✅ Migration to version 31 successful
```

## What the Script Does

### File Filtering
- ✅ **Only `.mp3` files** are included as lectures
- ❌ **`.json` files** are excluded (metadata only)
- ✅ **Speaker images** (`speaker.jpg`, `speaker.png`, `speaker_small.*`)
- ✅ **Collection covers** (`collection.jpg`, `collection.webp`, `collection_small.*`)

### URL Generation
All images and audio files get full CDN URLs:
- Speakers: `https://cdn.elmify.store/Speaker Name/speaker.jpg`
- Collections: `https://cdn.elmify.store/Speaker Name/Collection Name/collection.jpg`
- Lectures: `https://cdn.elmify.store/Speaker Name/Collection Name/01 - Title.mp3`

### Data Relationships
- Each lecture is linked to a speaker (via `speaker_id`)
- Each lecture is linked to a collection (via `collection_id`)
- Each collection is linked to a speaker (via `speaker_id`)

## Post-Deployment Actions

### ✅ Works Out of the Box
- Audio streaming to frontend
- Speaker images display
- Collection cover images display
- All 467 lectures are playable

### ⚠️ Manual Actions Required

#### 1. User Data is Lost
**What happens:** V24 deletes all user-specific data:
- Saved lectures
- Favorites
- Playback positions
- User activities

**Action:** This is intentional for a clean refresh. Users will need to re-add favorites and their playback positions will reset.

#### 2. Lecture IDs Change
**What happens:** All lecture IDs are reassigned (starting from 1)

**Action:** If you have any hardcoded lecture IDs in the frontend or external references, they will need to be updated.

#### 3. Verify Image Display
**Test:** Open the app and verify:
- Speaker profile images load
- Collection cover art loads
- No broken image icons

**If images don't load:**
- Check that CDN URLs are accessible: `https://cdn.elmify.store/Abdul Rashid Sufi/speaker.jpg`
- Verify R2 bucket has public access enabled for the CDN domain

#### 4. Verify Audio Playback
**Test:** Play a few lectures from different collections

**If audio doesn't play:**
- Check browser console for 404 errors
- Verify the audio URL format in the API response
- Check that `LectureController.java` is building CDN URLs correctly

## Troubleshooting

### Migration Fails with "Checksum Mismatch"

**Error:**
```
Flyway validation failed: Migration checksum mismatch for version 24
```

**Solution:**
The migration was already run. You need to either:
1. Delete V24-V31 and create V32-V39 (new version numbers)
2. Or wipe the Railway database completely and start fresh

To use new version numbers:
```bash
# In generate-migration-from-r2.js, change:
# V24 → V32
# V25 → V33
# etc.
```

### "No audio source available" Error

**Cause:** Database paths don't match actual R2 file structure

**Solution:**
1. Check the exact R2 path in your bucket
2. Verify the migration script generated the correct path
3. Re-run the script if needed

### Images Return 404

**Cause:** CDN URLs are malformed or files don't exist in R2

**Solution:**
1. Test a CDN URL directly in browser
2. Check R2 bucket for the exact file path
3. Verify speaker/collection images were uploaded to R2

### Deployment Takes Too Long (>10 minutes)

**Cause:** Too much data to migrate at once

**Solution:**
Railway has a 10-minute timeout. If deployment fails:
1. Split lectures into more parts (10 parts instead of 5)
2. Edit `generate-migration-from-r2.js` line 284:
   ```javascript
   const chunkSize = 50; // Reduce from 100 to 50
   ```
3. Regenerate migrations

## Database Schema Reference

### Speakers Table
```sql
id BIGSERIAL PRIMARY KEY
name TEXT NOT NULL UNIQUE
image_url TEXT (full CDN URL)
image_small_url TEXT (full CDN URL)
created_at TIMESTAMP
updated_at TIMESTAMP
```

### Collections Table
```sql
id BIGSERIAL PRIMARY KEY
speaker_id BIGINT (references speakers)
title TEXT NOT NULL
year INTEGER
cover_image_url TEXT (full CDN URL)
cover_image_small_url TEXT (full CDN URL)
created_at TIMESTAMP
updated_at TIMESTAMP
```

### Lectures Table
```sql
id BIGSERIAL PRIMARY KEY
title TEXT NOT NULL
file_name TEXT NOT NULL
file_path TEXT NOT NULL (relative path in R2)
file_size BIGINT NOT NULL
speaker_id BIGINT (references speakers)
collection_id BIGINT (references collections)
lecture_number INTEGER
duration INTEGER
is_public BOOLEAN
created_at TIMESTAMP
updated_at TIMESTAMP
```

## R2 Bucket Structure

Expected structure in your R2 bucket:

```
elmify-audio/
├── Speaker Name/
│   ├── speaker.jpg (or .png, .webp)
│   ├── speaker_small.jpg
│   └── Collection Name/
│       ├── collection.json (metadata)
│       ├── collection.jpg (or .png, .webp)
│       ├── collection_small.jpg
│       ├── 01 - Lecture Title.mp3
│       ├── 02 - Another Lecture.mp3
│       └── ...
```

### collection.json Format

```json
{
  "title": "Collection Title",
  "year": 2024,
  "description": "Optional description"
}
```

## Maintenance

### Adding New Lectures

1. Upload new `.mp3` files to R2 in the correct structure
2. Add/update `collection.json` if needed
3. Re-run this entire process to regenerate migrations
4. Push to deploy

### Updating Existing Lectures

Since V24 clears all data, you can safely:
1. Update files in R2
2. Re-run the import script
3. Deploy

All data will be refreshed from R2.

## Script Customization

### Change CDN Domain

Edit `scripts/generate-migration-from-r2.js` line 204:

```javascript
const CDN_BASE_URL = 'https://your-cdn.example.com/';
```

### Change Migration Version Numbers

Edit `scripts/generate-migration-from-r2.js` lines 237, 257, 271, 283:

```javascript
// Change V24-V31 to V32-V39 (or next available)
fs.writeFileSync(path.join(migrationsDir, 'V32__clean_database_for_r2_import.sql'), ...);
```

### Change Lecture Batch Size

Edit `scripts/generate-migration-from-r2.js` line 284:

```javascript
const chunkSize = 100; // Change to 50, 200, etc.
```

## Security Notes

- ✅ All audio files are publicly accessible via CDN (intended for free content model)
- ✅ Speaker/collection images are publicly accessible
- ⚠️ Do not commit `.env` file to Git (it's in `.gitignore`)
- ⚠️ R2 credentials in Railway are set as environment variables

## Summary

**Total time:** ~10-15 minutes
- Script execution: 30 seconds
- Build: 3-5 seconds
- Deployment + Migrations: 5-10 minutes

**Result:** Fresh database with all data from R2, ready to serve to frontend with images and audio streaming.
