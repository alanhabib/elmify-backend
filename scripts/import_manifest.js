#!/usr/bin/env node

/**
 * Import manifest.json data into PostgreSQL database
 *
 * Usage: node import_manifest.js [manifest_file]
 */

const fs = require('fs');
const { Client } = require('pg');

// Database connection config
// Use DATABASE_URL if available (for Railway), otherwise use local config
const DB_CONFIG = process.env.DATABASE_URL ? {
  connectionString: process.env.DATABASE_URL,
  ssl: {
    rejectUnauthorized: false
  }
} : {
  host: 'localhost',
  port: 5432,
  database: 'elmify_db',
  user: 'alanhabib',
  password: 'password'
};

// Read manifest file
const manifestFile = process.argv[2] || './manifest.json';
if (!fs.existsSync(manifestFile)) {
  console.error(`Error: Manifest file not found: ${manifestFile}`);
  process.exit(1);
}

const manifest = JSON.parse(fs.readFileSync(manifestFile, 'utf8'));

console.log(`📁 Reading manifest: ${manifestFile}`);
console.log(`📊 Found ${manifest.speakers.length} speakers`);

async function importData() {
  const client = new Client(DB_CONFIG);

  try {
    await client.connect();
    console.log('✅ Connected to database\n');

    let speakersInserted = 0;
    let collectionsInserted = 0;
    let lecturesInserted = 0;

    for (const speakerData of manifest.speakers) {
      const speakerName = speakerData.name.trim();
      const speakerBio = speakerData.bio?.trim() || null;
      // Store relative R2 paths - StorageService will generate presigned URLs at runtime
      const imageUrl = speakerData.imageUrl?.trim() || null;
      const imageSmallUrl = speakerData.imageSmallUrl?.trim() || null;
      const isPremium = speakerData.isPremium || false;

      console.log(`\n📢 Processing speaker: ${speakerName}`);

      // Insert or update speaker
      const speakerResult = await client.query(
        `INSERT INTO speakers (name, bio, image_url, image_small_url, is_premium, created_at, updated_at)
         VALUES ($1, $2, $3, $4, $5, NOW(), NOW())
         ON CONFLICT (name)
         DO UPDATE SET
           bio = EXCLUDED.bio,
           image_url = EXCLUDED.image_url,
           image_small_url = EXCLUDED.image_small_url,
           is_premium = EXCLUDED.is_premium,
           updated_at = NOW()
         RETURNING id`,
        [speakerName, speakerBio, imageUrl, imageSmallUrl, isPremium]
      );

      const speakerId = speakerResult.rows[0].id;
      speakersInserted++;
      console.log(`  ✅ Speaker ID: ${speakerId}`);

      // Process collections
      for (const collectionData of speakerData.collections || []) {
        const collectionTitle = collectionData.title?.trim();
        const collectionDescription = collectionData.description?.trim() || null;
        const collectionYear = collectionData.year || null;
        const coverImageUrl = collectionData.coverImageUrl?.trim() || null;
        const coverImageSmallUrl = collectionData.coverImageSmallUrl?.trim() || null;

        console.log(`  📚 Processing collection: ${collectionTitle}`);

        // Insert or update collection
        const collectionResult = await client.query(
          `INSERT INTO collections (speaker_id, title, description, year, cover_image_url, cover_image_small_url, created_at, updated_at)
           VALUES ($1, $2, $3, $4, $5, $6, NOW(), NOW())
           ON CONFLICT (speaker_id, title)
           DO UPDATE SET
             description = EXCLUDED.description,
             year = EXCLUDED.year,
             cover_image_url = EXCLUDED.cover_image_url,
             cover_image_small_url = EXCLUDED.cover_image_small_url,
             updated_at = NOW()
           RETURNING id`,
          [speakerId, collectionTitle, collectionDescription, collectionYear, coverImageUrl, coverImageSmallUrl]
        );

        const collectionId = collectionResult.rows[0].id;
        collectionsInserted++;
        console.log(`    ✅ Collection ID: ${collectionId}`);

        // Process lectures
        for (const lectureData of collectionData.lectures || []) {
          const lectureTitle = lectureData.title?.trim();
          const lectureDescription = lectureData.description?.trim() || null;
          const lectureNumber = lectureData.lectureNumber || 0;
          const fileName = lectureData.fileName?.trim();
          const filePath = lectureData.filePath?.trim();
          const duration = lectureData.duration || 0;
          const fileSize = lectureData.fileSize || 0;
          const fileFormat = lectureData.fileFormat?.trim() || 'mp3';
          const bitrate = lectureData.bitrate || 0;
          const sampleRate = lectureData.sampleRate || 0;
          const fileHash = lectureData.fileHash?.trim() || null;

          console.log(`    🎵 Processing lecture: ${lectureTitle}`);

          // Check if lecture exists, then insert or skip
          const existingLecture = await client.query(
            `SELECT id FROM lectures WHERE collection_id = $1 AND title = $2`,
            [collectionId, lectureTitle]
          );

          if (existingLecture.rows.length > 0) {
            console.log(`      ⏭️  Lecture already exists, skipping`);
            continue;
          }

          // Insert lecture (with speaker_id for proper relationship)
          await client.query(
            `INSERT INTO lectures (
              speaker_id, collection_id, title, description, lecture_number, file_name, file_path,
              duration, file_size, file_format, bitrate, sample_rate, file_hash,
              created_at, updated_at
             )
             VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, NOW(), NOW())`,
            [
              speakerId, collectionId, lectureTitle, lectureDescription, lectureNumber, fileName, filePath,
              duration, fileSize, fileFormat, bitrate, sampleRate, fileHash
            ]
          );

          lecturesInserted++;
          console.log(`      ✅ Lecture imported`);
        }
      }
    }

    console.log('\n' + '='.repeat(50));
    console.log('✅ Import complete!');
    console.log('='.repeat(50));
    console.log(`📊 Summary:`);
    console.log(`   Speakers:    ${speakersInserted}`);
    console.log(`   Collections: ${collectionsInserted}`);
    console.log(`   Lectures:    ${lecturesInserted}`);
    console.log('='.repeat(50));

  } catch (error) {
    console.error('\n❌ Error during import:', error.message);
    console.error(error.stack);
    process.exit(1);
  } finally {
    await client.end();
  }
}

// Run import
importData();

