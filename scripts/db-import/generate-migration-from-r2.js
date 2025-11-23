/**
 * Automated R2 to PostgreSQL Migration Generator
 *
 * This script:
 * 1. Lists all files in Cloudflare R2 bucket
 * 2. Parses folder structure (Speaker/Collection/Lectures)
 * 3. Reads collection.json metadata files
 * 4. Generates SQL migration files for PostgreSQL
 *
 * Required environment variables:
 * - R2_ACCOUNT_ID
 * - R2_ACCESS_KEY_ID
 * - R2_SECRET_ACCESS_KEY
 * - R2_BUCKET_NAME (optional, defaults to 'elmify-audio')
 */

const { S3Client, ListObjectsV2Command, GetObjectCommand } = require('@aws-sdk/client-s3');
const fs = require('fs');
const path = require('path');
require('dotenv').config();

// R2 Configuration
const R2_ENDPOINT = process.env.R2_ENDPOINT;
const R2_ACCESS_KEY_ID = process.env.R2_ACCESS_KEY_ID;
const R2_SECRET_ACCESS_KEY = process.env.R2_SECRET_ACCESS_KEY;
const R2_BUCKET_NAME = process.env.R2_BUCKET_NAME || 'elmify-audio';

if (!R2_ENDPOINT || !R2_ACCESS_KEY_ID || !R2_SECRET_ACCESS_KEY) {
  console.error('❌ Missing R2 credentials in environment variables');
  console.error('Required: R2_ENDPOINT, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY');
  process.exit(1);
}

// Initialize S3 client for R2
const s3Client = new S3Client({
  region: 'auto',
  endpoint: R2_ENDPOINT,
  credentials: {
    accessKeyId: R2_ACCESS_KEY_ID,
    secretAccessKey: R2_SECRET_ACCESS_KEY,
  },
  forcePathStyle: true,
});

/**
 * List all objects in R2 bucket
 */
async function listAllObjects() {
  const objects = [];
  let continuationToken = undefined;

  console.log(`📦 Scanning R2 bucket: ${R2_BUCKET_NAME}...`);

  do {
    const command = new ListObjectsV2Command({
      Bucket: R2_BUCKET_NAME,
      ContinuationToken: continuationToken,
    });

    const response = await s3Client.send(command);

    if (response.Contents) {
      objects.push(...response.Contents);
    }

    continuationToken = response.NextContinuationToken;
    console.log(`  Found ${objects.length} objects so far...`);
  } while (continuationToken);

  console.log(`✅ Total: ${objects.length} objects in R2`);
  return objects;
}

/**
 * Get object content from R2
 */
async function getObjectContent(key) {
  const command = new GetObjectCommand({
    Bucket: R2_BUCKET_NAME,
    Key: key,
  });

  const response = await s3Client.send(command);
  const bodyContents = await streamToString(response.Body);
  return bodyContents;
}

/**
 * Convert stream to string
 */
function streamToString(stream) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    stream.on('data', (chunk) => chunks.push(chunk));
    stream.on('error', reject);
    stream.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
  });
}

/**
 * Parse R2 folder structure
 * Expected: SpeakerName/CollectionName/files...
 */
function parseR2Structure(objects) {
  const structure = {};

  for (const obj of objects) {
    const key = obj.Key;
    const parts = key.split('/');

    const speakerName = parts[0];

    if (!structure[speakerName]) {
      structure[speakerName] = {
        speakerImage: null,
        speakerImageSmall: null,
        collections: {}
      };
    }

    // Speaker-level files (parts.length === 2)
    if (parts.length === 2) {
      const fileName = parts[1];
      if (fileName === 'speaker.jpg' || fileName === 'speaker.png' || fileName === 'speaker.webp') {
        structure[speakerName].speakerImage = key;
      } else if (fileName === 'speaker_small.jpg' || fileName === 'speaker_small.png' || fileName === 'speaker_small.webp') {
        structure[speakerName].speakerImageSmall = key;
      }
      continue;
    }

    // Collection-level files (parts.length >= 3)
    if (parts.length < 3) continue;

    const collectionName = parts[1];
    const fileName = parts[2];

    if (!structure[speakerName].collections[collectionName]) {
      structure[speakerName].collections[collectionName] = {
        files: [],
        metadata: null,
        coverImage: null,
        coverImageSmall: null,
      };
    }

    if (fileName) {
      if (fileName === 'collection.json') {
        structure[speakerName].collections[collectionName].metadataKey = key;
      } else if (fileName.endsWith('.mp3')) {
        structure[speakerName].collections[collectionName].files.push({
          name: fileName,
          key: key,
          size: obj.Size,
        });
      } else if (fileName.startsWith('collection') && (fileName.endsWith('.jpg') || fileName.endsWith('.webp') || fileName.endsWith('.png'))) {
        if (fileName.includes('_small')) {
          structure[speakerName].collections[collectionName].coverImageSmall = key;
        } else {
          structure[speakerName].collections[collectionName].coverImage = key;
        }
      }
    }
  }

  return structure;
}

/**
 * Load collection metadata from collection.json files
 */
async function loadMetadata(structure) {
  console.log('\n📋 Loading collection metadata...');

  for (const [speakerName, speakerData] of Object.entries(structure)) {
    for (const [collectionName, data] of Object.entries(speakerData.collections)) {
      if (data.metadataKey) {
        try {
          const content = await getObjectContent(data.metadataKey);
          data.metadata = JSON.parse(content);
          console.log(`  ✓ ${speakerName} / ${collectionName}`);
        } catch (error) {
          console.warn(`  ⚠ Failed to load metadata for ${speakerName}/${collectionName}`);
        }
      }
    }
  }
}

/**
 * Generate SQL migration data
 */
function generateSQL(structure) {
  let speakerId = 1;
  let collectionId = 1;
  let lectureId = 1;

  const speakers = [];
  const collections = [];
  const lectures = [];

  console.log('\n🔨 Generating SQL...');

  const CDN_BASE_URL = 'https://cdn.elmify.store/';

  for (const [speakerName, speakerData] of Object.entries(structure)) {
    const currentSpeakerId = speakerId++;
    const speakerImage = speakerData.speakerImage ? `'${CDN_BASE_URL}${speakerData.speakerImage}'` : 'NULL';
    const speakerImageSmall = speakerData.speakerImageSmall ? `'${CDN_BASE_URL}${speakerData.speakerImageSmall}'` : 'NULL';

    // Insert speaker with images
    speakers.push(`(${currentSpeakerId}, '${escapeSql(speakerName)}', NOW(), NOW(), ${speakerImage}, ${speakerImageSmall}, 'public', NULL, FALSE)`);

    for (const [collectionName, data] of Object.entries(speakerData.collections)) {
      const currentCollectionId = collectionId++;
      const year = data.metadata?.year || new Date().getFullYear();
      const coverImage = data.coverImage ? `'${CDN_BASE_URL}${data.coverImage}'` : 'NULL';
      const coverImageSmall = data.coverImageSmall ? `'${CDN_BASE_URL}${data.coverImageSmall}'` : 'NULL';

      // Insert collection with images
      collections.push(`(${currentCollectionId}, ${currentSpeakerId}, '${escapeSql(collectionName)}', ${year}, ${coverImage}, NOW(), NOW(), ${coverImageSmall})`);

      // Sort audio files by name
      const audioFiles = data.files.sort((a, b) => a.name.localeCompare(b.name));

      for (let i = 0; i < audioFiles.length; i++) {
        const file = audioFiles[i];
        const currentLectureId = lectureId++;

        // Extract lecture title from filename
        // Pattern: "XX - Title.mp3" -> "Title"
        const titleMatch = file.name.match(/^\d+\s*-\s*(.+)\.mp3$/);
        const title = titleMatch ? titleMatch[1].trim() : file.name.replace('.mp3', '');

        // File path for R2
        const filePath = `${speakerName}/${collectionName}/${file.name}`;

        lectures.push(
          `(${currentLectureId}, '${escapeSql(title)}', NULL, ${year}, 0, '${escapeSql(file.name)}', '${escapeSql(filePath)}', ${file.size}, 'audio/mpeg', NULL, NULL, NULL, NULL, NULL, TRUE, 0, NOW(), NULL, NOW(), NOW(), ${currentSpeakerId}, ${currentCollectionId}, NULL, ${i + 1})`
        );
      }
    }
  }

  console.log(`  ✓ ${speakers.length} speakers`);
  console.log(`  ✓ ${collections.length} collections`);
  console.log(`  ✓ ${lectures.length} lectures`);

  return { speakers, collections, lectures };
}

/**
 * Escape SQL strings
 */
function escapeSql(str) {
  return str.replace(/'/g, "''");
}

/**
 * Write SQL migration files
 */
function writeSQLFiles(data) {
  const migrationsDir = path.join(__dirname, '..', 'src', 'main', 'resources', 'db', 'migration');

  // V32: Clean database
  const v32Content = `-- Generated on ${new Date().toISOString()}
-- Clean all existing data before fresh R2 import

BEGIN;

-- Delete dependent data first
DELETE FROM user_saved_lectures;
DELETE FROM favorites;
DELETE FROM playback_positions;
DELETE FROM user_activities;

-- Delete main entities
DELETE FROM lectures;
DELETE FROM collections;
DELETE FROM speakers;

COMMIT;
`;

  // V33: Insert speakers
  const v33Content = `-- Generated on ${new Date().toISOString()}
-- Insert speakers from R2

BEGIN;

INSERT INTO speakers (id, name, created_at, updated_at, image_url, image_small_url, visibility_type, allowed_user_ids, is_premium)
VALUES
${data.speakers.join(',\n')};

COMMIT;
`;

  // V34: Insert collections
  const v34Content = `-- Generated on ${new Date().toISOString()}
-- Insert collections from R2

BEGIN;

INSERT INTO collections (id, speaker_id, title, year, cover_image_url, created_at, updated_at, cover_image_small_url)
VALUES
${data.collections.join(',\n')};

COMMIT;
`;

  // V35: Insert lectures (split into chunks if too large)
  const chunkSize = 100;
  const lectureChunks = [];
  for (let i = 0; i < data.lectures.length; i += chunkSize) {
    lectureChunks.push(data.lectures.slice(i, i + chunkSize));
  }

  // Write files
  fs.writeFileSync(path.join(migrationsDir, 'V32__clean_database_for_r2_import.sql'), v32Content);
  fs.writeFileSync(path.join(migrationsDir, 'V33__insert_speakers_from_r2.sql'), v33Content);
  fs.writeFileSync(path.join(migrationsDir, 'V34__insert_collections_from_r2.sql'), v34Content);

  // Write lecture migrations (can be multiple parts if many lectures)
  for (let i = 0; i < lectureChunks.length; i++) {
    const partNum = i + 1;
    const versionNum = 35 + i;
    const partContent = `-- Generated on ${new Date().toISOString()}
-- Insert lectures from R2 (Part ${partNum} of ${lectureChunks.length})

BEGIN;

INSERT INTO lectures (id, title, genre, year, duration, file_name, file_path, file_size, file_format, bitrate, sample_rate, file_hash, thumbnail_url, waveform_data, is_public, play_count, uploaded_at, last_played_at, created_at, updated_at, speaker_id, collection_id, description, lecture_number)
VALUES
${lectureChunks[i].join(',\n')};

COMMIT;
`;
    fs.writeFileSync(path.join(migrationsDir, `V${versionNum}__insert_lectures_from_r2_part${partNum}.sql`), partContent);
  }

  console.log(`\n✅ Generated migration files in:`);
  console.log(`   ${migrationsDir}`);
  console.log(`   - V32: Clean database`);
  console.log(`   - V33: Insert ${data.speakers.length} speakers`);
  console.log(`   - V34: Insert ${data.collections.length} collections`);
  console.log(`   - V35-V${34 + lectureChunks.length}: Insert ${data.lectures.length} lectures (${lectureChunks.length} parts)`);
}

/**
 * Main execution
 */
async function main() {
  try {
    console.log('🚀 Starting R2 to PostgreSQL migration generator\n');

    // Step 1: List all objects
    const objects = await listAllObjects();

    // Step 2: Parse structure
    console.log('\n📁 Parsing folder structure...');
    const structure = parseR2Structure(objects);
    console.log(`✅ Found ${Object.keys(structure).length} speakers`);

    // Step 3: Load metadata
    await loadMetadata(structure);

    // Step 4: Generate SQL
    const sqlData = generateSQL(structure);

    // Step 5: Write files
    writeSQLFiles(sqlData);

    console.log('\n🎉 Migration files generated successfully!');
    console.log('\nNext steps:');
    console.log('  1. Review generated files in src/main/resources/db/migration/');
    console.log('  2. Build: JAVA_HOME=/path/to/java21 ./mvnw clean package -DskipTests');
    console.log('  3. Commit and push to deploy to Railway');

  } catch (error) {
    console.error('\n❌ Error:', error.message);
    console.error(error);
    process.exit(1);
  }
}

main();
