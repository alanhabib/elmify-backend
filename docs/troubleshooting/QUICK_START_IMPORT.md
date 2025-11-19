# 🚀 Quick Start: Import R2 Data to PostgreSQL

**Use this guide for the simplest path to import your R2 data.**

---

## ✅ Prerequisites

- [x] R2 credentials available
- [x] Railway PostgreSQL connection string
- [x] Node.js installed
- [x] Scripts in `elmify-backend/scripts/` directory

---

## 🎯 One Command Import (Railway)

```bash
cd scripts

# Set your Railway DATABASE_URL
export DATABASE_URL="postgresql://postgres:xxx@xxx.railway.app:5432/railway"

# Import the corrected manifest
node import_manifest.js r2_manifest_fixed.json
```

**That's it!** ✅

---

## 📋 If You Need to Regenerate the Manifest

Only do this if you've added new lectures to R2:

```bash
cd scripts

# Regenerate from R2
node import_build_R2_manifest_json.js \
  --endpoint https://b995be98e08909685abfca00c971e79e.r2.cloudflarestorage.com \
  --access-key YOUR_ACCESS_KEY \
  --secret-key YOUR_SECRET_KEY \
  --bucket elmify-audio \
  --output r2_manifest_$(date +%Y%m%d).json

# Then import the new manifest
DATABASE_URL="..." node import_manifest.js r2_manifest_$(date +%Y%m%d).json
```

---

## 🔍 Verify Import

```bash
# Connect to Railway PostgreSQL
railway connect postgres
```

Then run:
```sql
-- Count records
SELECT COUNT(*) FROM speakers;    -- Should be 8
SELECT COUNT(*) FROM collections; -- Should be 12
SELECT COUNT(*) FROM lectures;    -- Should be 467

-- View data
SELECT name FROM speakers ORDER BY name;
SELECT title, speaker_id FROM collections;
SELECT title, collection_id FROM lectures LIMIT 10;

-- Exit
\q
```

---

## 📦 What Gets Imported

| Table | Records | Fields Imported |
|-------|---------|-----------------|
| **speakers** | 8 | name, bio, imageUrl, imageSmallUrl, isPremium |
| **collections** | 12 | title, description, year, coverImageUrl, coverImageSmallUrl |
| **lectures** | 467 | title, fileName, filePath, fileSize, fileFormat, lectureNumber |

---

## ⚠️ Important Notes

### Use the Correct Manifest
- ✅ **Use:** `r2_manifest_fixed.json` (has `coverImageSmallUrl`)
- ❌ **Don't use:** `r2_manifest_1.json` (has wrong field name)

### Database URLs Are Stored as Relative Paths
The database stores paths like:
```
Abdul Rashid Sufi/speaker.jpg
```

Not full URLs. The backend generates presigned URLs at runtime.

### Re-running is Safe
The import script uses `ON CONFLICT` clauses, so you can run it multiple times without creating duplicates.

---

## 🐛 Troubleshooting

**"Connection refused"**
```bash
# Check DATABASE_URL is set
echo $DATABASE_URL

# Get it from Railway
railway variables
```

**"Manifest file not found"**
```bash
# Make sure you're in the scripts directory
cd scripts
ls -la r2_manifest_fixed.json
```

**"Column 'bio' does not exist"**
```bash
# Flyway migrations haven't run yet
# Wait for Railway deployment to complete
railway logs
```

---

## 📚 Full Documentation

For detailed information, see:
- `docs/troubleshooting/COMPLETE-RESOLUTION-SUMMARY.md` - Complete overview
- `docs/troubleshooting/r2-postgresql-import-analysis.md` - Detailed analysis
- `docs/troubleshooting/field-mapping-verification.md` - Field compatibility

---

**Ready to import? Run the command above!** 🚀

