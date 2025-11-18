# R2 to PostgreSQL Migration Generator

Automatically generates SQL migration files from your Cloudflare R2 bucket.

## Prerequisites

1. **Cloudflare R2 API Credentials**
   - Go to: https://dash.cloudflare.com/
   - Navigate to: R2 → Manage R2 API Tokens
   - Create a new API token with read permissions
   - Note down:
     - Account ID
     - Access Key ID
     - Secret Access Key

2. **Node.js 18+** installed

## Setup

1. Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env` and fill in your R2 credentials:
   ```env
   R2_ACCOUNT_ID=your_account_id
   R2_ACCESS_KEY_ID=your_access_key
   R2_SECRET_ACCESS_KEY=your_secret_key
   R2_BUCKET_NAME=elmify-audio
   ```

3. Install dependencies (already done):
   ```bash
   npm install
   ```

## Usage

Run the script:

```bash
cd scripts
node generate-migration-from-r2.js
```

This will:
1. Scan your R2 bucket (`elmify-audio`)
2. Parse the folder structure (Speaker/Collection/Lectures)
3. Read `collection.json` metadata files
4. Generate SQL migration files in `src/main/resources/db/migration/`:
   - `V16__clean_database_for_r2_import.sql` - Cleans existing data
   - `V17__insert_speakers_from_r2.sql` - Inserts speakers
   - `V18__insert_collections_from_r2.sql` - Inserts collections
   - `V19__insert_lectures_from_r2_part1.sql` - Inserts lectures (can be multiple parts)

## Expected R2 Structure

```
elmify-audio/
├── Speaker Name 1/
│   ├── Collection Name 1/
│   │   ├── collection.json (optional metadata)
│   │   ├── collection.jpg (optional cover image)
│   │   ├── 01 - Lecture Title 1.mp3
│   │   ├── 02 - Lecture Title 2.mp3
│   │   └── ...
│   └── Collection Name 2/
│       └── ...
└── Speaker Name 2/
    └── ...
```

## After Generation

1. Review the generated files in `src/main/resources/db/migration/`
2. Delete old migrations (V2-V15) if they conflict
3. Build the project:
   ```bash
   cd ..
   ./mvnw clean package -DskipTests
   ```
4. Commit and push to deploy:
   ```bash
   git add .
   git commit -m "Regenerate database from R2"
   git push
   ```

Railway will automatically deploy and run the new migrations!

## Troubleshooting

**Error: Missing R2 credentials**
- Make sure `.env` file exists and has all required variables

**Error: Access denied**
- Check your R2 API token has read permissions
- Verify the bucket name is correct

**Error: No objects found**
- Verify your bucket name in `.env`
- Check that files exist in R2
