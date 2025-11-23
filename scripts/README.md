# Elmify Content Management Scripts

Scripts for managing content upload to Cloudflare R2 and database synchronization.

## Directory Structure

```
scripts/
├── r2-upload/          # Upload content to R2 storage
├── db-import/          # Sync R2 metadata to PostgreSQL
├── utilities/          # Helper scripts
├── .env                # R2 credentials (copy from .env.example)
├── package.json        # Node.js dependencies
└── README.md
```

## Setup

```bash
cd scripts
cp .env.example .env
# Edit .env with your R2 credentials
npm install
```

## Workflows

### 1. Upload New Content to R2

```bash
# Create speaker template
./r2-upload/create_speaker.sh "Speaker Name" "Collection Name" ~/content

# Add audio files, then validate
./r2-upload/validate_content.sh ~/content

# Fix any issues
./r2-upload/fix_content.sh ~/content

# Upload to R2
./r2-upload/upload_to_r2.sh ~/content
```

### 2. Sync R2 to Database

```bash
# Generate manifest from R2
node db-import/scan_r2_and_sync.js

# Import to PostgreSQL
DATABASE_URL="postgresql://..." node db-import/import_manifest.js r2_scanned_manifest.json
```

### 3. Unified Workflow (Recommended)

```bash
# Push: Upload to R2 + sync to database
DATABASE_URL="postgresql://..." ./r2-upload/sync_metadata.sh push --dir ~/content

# Pull: Export database to manifest
DATABASE_URL="postgresql://..." ./r2-upload/sync_metadata.sh pull

# Status: Check current state
./r2-upload/sync_metadata.sh status
```

## Scripts Reference

### r2-upload/

| Script | Description |
|--------|-------------|
| `upload_to_r2.sh` | Upload content directory to R2 |
| `create_speaker.sh` | Create speaker/collection template |
| `validate_content.sh` | Validate directory structure |
| `fix_content.sh` | Auto-fix common issues |
| `clear_r2_storage.sh` | Clear R2 bucket |
| `generate_small_images.sh` | Generate thumbnail images |
| `sync_metadata.sh` | Unified push/pull/scan workflow |

### db-import/

| Script | Description |
|--------|-------------|
| `import_manifest.js` | Import manifest JSON to PostgreSQL |
| `scan_r2_and_sync.js` | Scan R2 bucket → generate manifest |
| `scan_legacy_r2_structure.js` | Scan legacy R2 structure |
| `generate_manifest.js` | Generate manifest from local files |
| `generate-migration-from-r2.js` | Generate SQL migrations from R2 |
| `export_from_db.js` | Export database to manifest |
| `download_from_r2.js` | Download files from R2 |
| `import_build_R2_manifest_json.js` | Build manifest from R2 |
| `fix_missing_speaker_ids.js` | Fix data issues |

### utilities/

| Script | Description |
|--------|-------------|
| `rename_quran_surahs.js` | Rename Quran surah files |
| `convert_existing_structure.sh` | Convert old structure to new |

## Expected Content Structure

```
content/
├── Speaker Name/
│   ├── speaker.json          # Speaker metadata
│   ├── speaker.jpg           # Speaker image
│   ├── speaker_small.jpg     # Speaker thumbnail
│   └── Collection Name/
│       ├── collection.json   # Collection metadata
│       ├── collection.jpg    # Cover image
│       ├── collection_small.jpg
│       ├── 01 - Lecture Title.mp3
│       └── 02 - Another Lecture.mp3
```

### collection.json format

```json
{
  "title": "Collection Title",
  "description": "Description of the collection",
  "year": 2023,
  "cover": "collection.jpg",
  "coverSmall": "collection_small.jpg",
  "categorySlug": "spirituality"
}
```

## Environment Variables

```env
R2_ENDPOINT=https://xxx.r2.cloudflarestorage.com
R2_ACCESS_KEY_ID=your_access_key
R2_SECRET_ACCESS_KEY=your_secret_key
R2_BUCKET_NAME=elmify-audio
```

## Dependencies

```bash
# Node.js packages (handled by npm install)
npm install

# System tools
brew install ffmpeg imagemagick jq
npm install -g wrangler
```
