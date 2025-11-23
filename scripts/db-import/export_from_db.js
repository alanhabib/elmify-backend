#!/usr/bin/env node

/**
 * Export PostgreSQL database to manifest.json
 *
 * This script downloads all speaker, collection, and lecture metadata
 * from your Railway PostgreSQL database and generates a manifest.json file.
 *
 * Usage:
 *   # Export from Railway production database
 *   DATABASE_URL="postgresql://..." node export_from_db.js
 *
 *   # Export from local database
 *   node export_from_db.js
 *
 *   # Specify custom output file
 *   node export_from_db.js exported_manifest.json
 */

const fs = require("fs");
const { Client } = require("pg");

// Database connection config
// Use DATABASE_URL if available (for Railway), otherwise use local config
const DB_CONFIG = process.env.DATABASE_URL
  ? {
      connectionString: process.env.DATABASE_URL,
      ssl: {
        rejectUnauthorized: false,
      },
    }
  : {
      host: "localhost",
      port: 5432,
      database: "elmify_db",
      user: "alanhabib",
      password: "password",
    };

// Output file
const outputFile = process.argv[2] || "./exported_manifest.json";

console.log("📡 Connecting to database...");
console.log(`📝 Output file: ${outputFile}\n`);

async function exportData() {
  const client = new Client(DB_CONFIG);

  try {
    await client.connect();
    console.log("✅ Connected to database\n");

    const manifest = {
      exportedAt: new Date().toISOString(),
      speakers: [],
    };

    // Fetch all speakers
    const speakersResult = await client.query(
      `SELECT id, name, bio, image_url, image_small_url, is_premium 
       FROM speakers 
       ORDER BY name`
    );

    console.log(`📢 Found ${speakersResult.rows.length} speakers\n`);

    for (const speaker of speakersResult.rows) {
      console.log(`Processing: ${speaker.name}`);

      const speakerData = {
        name: speaker.name,
        bio: speaker.bio,
        imageUrl: speaker.image_url,
        imageSmallUrl: speaker.image_small_url,
        isPremium: speaker.is_premium,
        collections: [],
      };

      // Fetch collections for this speaker
      const collectionsResult = await client.query(
        `SELECT id, title, description, year, cover_image_url, cover_image_small_url
         FROM collections
         WHERE speaker_id = $1
         ORDER BY title`,
        [speaker.id]
      );

      console.log(`  Collections: ${collectionsResult.rows.length}`);

      for (const collection of collectionsResult.rows) {
        const collectionData = {
          title: collection.title,
          description: collection.description,
          year: collection.year,
          coverImageUrl: collection.cover_image_url,
          coverImageSmallUrl: collection.cover_image_small_url,
          lectures: [],
        };

        // Fetch lectures for this collection
        const lecturesResult = await client.query(
          `SELECT 
            title, description, lecture_number, file_name, file_path,
            duration, file_size, file_format, bitrate, sample_rate, file_hash
           FROM lectures
           WHERE collection_id = $1
           ORDER BY lecture_number`,
          [collection.id]
        );

        console.log(`    Lectures: ${lecturesResult.rows.length}`);

        for (const lecture of lecturesResult.rows) {
          collectionData.lectures.push({
            title: lecture.title,
            description: lecture.description,
            lectureNumber: lecture.lecture_number,
            fileName: lecture.file_name,
            filePath: lecture.file_path,
            duration: lecture.duration,
            fileSize: lecture.file_size,
            fileFormat: lecture.file_format,
            bitrate: lecture.bitrate,
            sampleRate: lecture.sample_rate,
            fileHash: lecture.file_hash,
          });
        }

        speakerData.collections.push(collectionData);
      }

      manifest.speakers.push(speakerData);
    }

    // Write manifest file
    fs.writeFileSync(outputFile, JSON.stringify(manifest, null, 2));

    console.log("\n" + "=".repeat(60));
    console.log("✅ Export complete!");
    console.log("=".repeat(60));
    console.log(`📊 Summary:`);
    console.log(`   Speakers:    ${manifest.speakers.length}`);
    console.log(
      `   Collections: ${manifest.speakers.reduce(
        (sum, s) => sum + s.collections.length,
        0
      )}`
    );
    console.log(
      `   Lectures:    ${manifest.speakers.reduce(
        (sum, s) =>
          sum + s.collections.reduce((cSum, c) => cSum + c.lectures.length, 0),
        0
      )}`
    );
    console.log("=".repeat(60));
    console.log(`📁 Saved to: ${outputFile}`);
  } catch (error) {
    console.error("\n❌ Error during export:", error.message);
    console.error(error.stack);
    process.exit(1);
  } finally {
    await client.end();
  }
}

// Run export
exportData();
