#!/usr/bin/env node

/**
 * Scan R2 bucket using AWS SDK and generate JSON manifest
 *
 * This script:
 * 1. Lists all files in Cloudflare R2 bucket
 * 2. Parses folder structure (Speaker/Collection/Lectures)
 * 3. Reads collection.json metadata files
 * 4. Generates JSON manifest for import_manifest.js
 *
 * Usage with arguments (recommended):
 *   node scan_r2_with_sdk.js \
 *     --endpoint https://xxx.r2.cloudflarestorage.com \
 *     --access-key YOUR_ACCESS_KEY \
 *     --secret-key YOUR_SECRET_KEY \
 *     --bucket elmify-audio \
 *     --output r2_manifest.json
 *
 * Or with environment variables:
 *   R2_ENDPOINT=https://xxx.r2.cloudflarestorage.com \
 *   R2_ACCESS_KEY_ID=xxx \
 *   R2_SECRET_ACCESS_KEY=xxx \
 *   R2_BUCKET_NAME=elmify-audio \
 *   node scan_r2_with_sdk.js
 */

const { S3Client, ListObjectsV2Command, GetObjectCommand } = require('@aws-sdk/client-s3');
const fs = require('fs');
const path = require('path');

// Parse command line arguments
const args = process.argv.slice(2);
function getArg(name, envVar, defaultValue) {
  // Try format: --name=value
  const argWithEquals = args.find(arg => arg.startsWith(`--${name}=`));
  if (argWithEquals) {
    return argWithEquals.split('=')[1];
  }

  // Try format: --name value
  const argIndex = args.findIndex(arg => arg === `--${name}`);
  if (argIndex !== -1 && argIndex + 1 < args.length) {
    return args[argIndex + 1];
  }

  return process.env[envVar] || defaultValue;
}

const R2_ENDPOINT = getArg('endpoint', 'R2_ENDPOINT');
const R2_ACCESS_KEY_ID = getArg('access-key', 'R2_ACCESS_KEY_ID');
const R2_SECRET_ACCESS_KEY = getArg('secret-key', 'R2_SECRET_ACCESS_KEY');
const R2_BUCKET_NAME = getArg('bucket', 'R2_BUCKET_NAME', 'elmify-audio');
const OUTPUT_FILE = getArg('output', 'OUTPUT_FILE', 'r2_sdk_manifest.json');

console.log('🔍 R2 to JSON Manifest Generator');
console.log('━'.repeat(60));
console.log(`🪣 Bucket: ${R2_BUCKET_NAME}`);
console.log(`🔗 Endpoint: ${R2_ENDPOINT ? R2_ENDPOINT.substring(0, 40) + '...' : 'Not set'}`);
console.log(`📝 Output: ${OUTPUT_FILE}`);
console.log('━'.repeat(60));
console.log('');

if (!R2_ENDPOINT || !R2_ACCESS_KEY_ID || !R2_SECRET_ACCESS_KEY) {
  console.error('❌ Missing R2 credentials\n');
  console.error('Usage with arguments:');
  console.error('  node scan_r2_with_sdk.js \\');
  console.error('    --endpoint https://xxx.r2.cloudflarestorage.com \\');
  console.error('    --access-key YOUR_ACCESS_KEY \\');
  console.error('    --secret-key YOUR_SECRET_KEY \\');
  console.error('    --bucket elmify-audio \\');
  console.error('    --output r2_manifest.json\n');
  console.error('Or with environment variables:');
  console.error('  R2_ENDPOINT, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY, R2_BUCKET_NAME');
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

  console.log('📦 Scanning R2 bucket...');

  try {
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

    console.log(`✅ Total: ${objects.length} objects in R2\n`);
    return objects;
  } catch (error) {
    console.error('\n❌ Error listing R2 objects:', error.message);
    console.error('\nCommon issues:');
    console.error('  1. Check your endpoint URL is correct');
    console.error('  2. Verify access key and secret key');
    console.error('  3. Ensure bucket name is correct');
    console.error('  4. Check bucket permissions');
    throw error;
  }
}

/**
 * Get object content from R2
 */
async function getObjectContent(key) {
  try {
    const command = new GetObjectCommand({
      Bucket: R2_BUCKET_NAME,
      Key: key,
    });

    const response = await s3Client.send(command);
    const bodyContents = await streamToString(response.Body);
    return bodyContents;
  } catch (error) {
    console.warn(`  ⚠️  Failed to read ${key}: ${error.message}`);
    return null;
  }
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
 * Helper: parse lecture filename
 */
function parseLectureFilename(filename) {
  // Remove extension
  const nameWithoutExt = filename.replace(/\.(mp3|m4a|wav)$/i, '');

  // Try to extract lecture number (e.g., "01 - Title" or "1 - Title")
  const match = nameWithoutExt.match(/^(\d+)\s*[-–—]\s*(.*)/);

  if (match) {
    return {
      lectureNumber: parseInt(match[1], 10),
      title: match[2].trim(),
    };
  }

  return {
    lectureNumber: 0,
    title: nameWithoutExt.trim(),
  };
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

    if (parts.length < 2) {
      console.log(`⏭️  Skipping root file: ${key}`);
      continue;
    }

    const speakerName = parts[0];

    if (!structure[speakerName]) {
      structure[speakerName] = {
        speakerImage: null,
        speakerImageSmall: null,
        speakerJsonKey: null,
        collections: {}
      };
    }

    // Speaker-level files (parts.length === 2)
    if (parts.length === 2) {
      const fileName = parts[1];
      if (fileName === 'speaker.json') {
        structure[speakerName].speakerJsonKey = key;
      } else if (fileName === 'speaker.jpg' || fileName === 'speaker.png' || fileName === 'speaker.webp' || fileName === 'speaker.jpeg') {
        structure[speakerName].speakerImage = key;
      } else if (fileName.match(/^speaker_small\.(jpg|png|webp|jpeg)$/)) {
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
        metadataKey: null,
        metadata: null,
        coverImage: null,
        coverImageSmall: null,
      };
    }

    if (fileName) {
      if (fileName === 'collection.json') {
        structure[speakerName].collections[collectionName].metadataKey = key;
      } else if (fileName.match(/\.(mp3|m4a|wav)$/i)) {
        structure[speakerName].collections[collectionName].files.push({
          name: fileName,
          key: key,
          size: obj.Size,
        });
      } else if (fileName.startsWith('collection') && fileName.match(/\.(jpg|webp|png|jpeg)$/)) {
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
  console.log('📋 Loading metadata from JSON files...\n');

  // Load speaker metadata
  for (const [speakerName, speakerData] of Object.entries(structure)) {
    if (speakerData.speakerJsonKey) {
      try {
        const content = await getObjectContent(speakerData.speakerJsonKey);
        if (content) {
          speakerData.speakerMetadata = JSON.parse(content);
          console.log(`  ✓ Speaker: ${speakerName}`);
        }
      } catch (error) {
        console.warn(`  ⚠️  Failed to parse speaker.json for ${speakerName}`);
      }
    }
  }

  console.log('');

  // Load collection metadata
  for (const [speakerName, speakerData] of Object.entries(structure)) {
    for (const [collectionName, data] of Object.entries(speakerData.collections)) {
      if (data.metadataKey) {
        try {
          const content = await getObjectContent(data.metadataKey);
          if (content) {
            data.metadata = JSON.parse(content);
            console.log(`  ✓ ${speakerName} / ${collectionName}`);
          }
        } catch (error) {
          console.warn(`  ⚠️  Failed to parse collection.json for ${speakerName}/${collectionName}`);
        }
      }
    }
  }

  console.log('');
}

/**
 * Generate JSON manifest from parsed structure
 */
function generateManifest(structure) {
  console.log('🔨 Generating JSON manifest...\n');

  const manifest = {
    scannedAt: new Date().toISOString(),
    source: 'r2-aws-sdk-scan',
    structure: 'flat (Speaker/Collection/file.mp3)',
    bucketName: R2_BUCKET_NAME,
    speakers: [],
  };

  for (const [speakerName, speakerData] of Object.entries(structure)) {
    const speaker = {
      name: speakerName,
      bio: speakerData.speakerMetadata?.bio || null,
      imageUrl: speakerData.speakerImage || null,
      imageSmallUrl: speakerData.speakerImageSmall || null,
      isPremium: speakerData.speakerMetadata?.isPremium || false,
      collections: [],
    };

    for (const [collectionName, data] of Object.entries(speakerData.collections)) {
      // Sort audio files by name
      const audioFiles = data.files.sort((a, b) => a.name.localeCompare(b.name));

      const collection = {
        title: collectionName,
        description: data.metadata?.description || null,
        year: data.metadata?.year || new Date().getFullYear(),
        coverImageUrl: data.coverImage || null,
        coverImageSmallUrl: data.coverImageSmall || null,
        lectures: [],
      };

      for (const file of audioFiles) {
        const { lectureNumber, title } = parseLectureFilename(file.name);

        collection.lectures.push({
          title: title,
          description: null,
          lectureNumber: lectureNumber,
          fileName: file.name,
          filePath: file.key,
          duration: 0,
          fileSize: file.size,
          fileFormat: path.extname(file.name).slice(1).toLowerCase() || 'mp3',
        });
      }

      speaker.collections.push(collection);
    }

    // Sort collections by title
    speaker.collections.sort((a, b) => a.title.localeCompare(b.title));

    manifest.speakers.push(speaker);
  }

  // Sort speakers by name
  manifest.speakers.sort((a, b) => a.name.localeCompare(b.name));

  console.log(`  ✓ ${manifest.speakers.length} speakers`);
  console.log(`  ✓ ${manifest.speakers.reduce((sum, s) => sum + s.collections.length, 0)} collections`);
  console.log(`  ✓ ${manifest.speakers.reduce((sum, s) => sum + s.collections.reduce((cSum, c) => cSum + c.lectures.length, 0), 0)} lectures`);

  return manifest;
}

/**
 * Write manifest to JSON file
 */
function writeManifestFile(manifest) {
  fs.writeFileSync(OUTPUT_FILE, JSON.stringify(manifest, null, 2));

  const totalCollections = manifest.speakers.reduce((sum, s) => sum + s.collections.length, 0);
  const totalLectures = manifest.speakers.reduce(
    (sum, s) => sum + s.collections.reduce((cSum, c) => cSum + c.lectures.length, 0),
    0
  );

  console.log('\n━'.repeat(60));
  console.log('✅ Manifest generated successfully!');
  console.log('━'.repeat(60));
  console.log(`📊 Summary:`);
  console.log(`   Speakers:    ${manifest.speakers.length}`);
  console.log(`   Collections: ${totalCollections}`);
  console.log(`   Lectures:    ${totalLectures}`);
  console.log('━'.repeat(60));
  console.log(`📁 Saved to: ${OUTPUT_FILE}`);
  console.log('');
  console.log('📋 Speakers found:');
  manifest.speakers.forEach((s) => {
    console.log(`   • ${s.name} (${s.collections.length} collections)`);
  });
  console.log('');
  console.log('⚠️  NEXT STEPS:');
  console.log('   1. Review the manifest file (add bios, descriptions if needed)');
  console.log('   2. Import to PostgreSQL:');
  console.log(`      DATABASE_URL="postgresql://..." node import_manifest.js ${OUTPUT_FILE}`);
  console.log('');
}

/**
 * Main execution
 */
async function main() {
  try {
    console.log('🚀 Starting R2 scan...\n');

    // Step 1: List all objects
    const objects = await listAllObjects();

    // Step 2: Parse structure
    console.log('📁 Parsing folder structure...');
    const structure = parseR2Structure(objects);
    console.log(`✅ Found ${Object.keys(structure).length} speakers\n`);

    // Step 3: Load metadata from JSON files
    await loadMetadata(structure);

    // Step 4: Generate manifest
    const manifest = generateManifest(structure);

    // Step 5: Write to file
    writeManifestFile(manifest);

  } catch (error) {
    console.error('\n❌ Error:', error.message);
    console.error(error);
    process.exit(1);
  }
}

main();
