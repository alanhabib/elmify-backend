#!/usr/bin/env node

/**
 * Scan R2 bucket and generate metadata for database import
 *
 * This script scans your R2 bucket structure and generates metadata
 * based on file paths and names. Useful when files are in R2 but
 * metadata is not in the database.
 *
 * WARNING: This generates BASIC metadata from file names.
 * You'll need to manually add bios, descriptions, etc.
 *
 * Usage:
 *   node scan_r2_and_sync.js [bucket_name]
 *
 * Example:
 *   node scan_r2_and_sync.js elmify-audio
 */

const { execSync } = require("child_process");
const fs = require("fs");
const path = require("path");

const bucketName =
  process.argv[2] || process.env.R2_BUCKET_NAME || "elmify-audio";
const outputFile = "r2_scanned_manifest.json";

console.log("🔍 R2 Bucket Scanner");
console.log("━".repeat(60));
console.log(`🪣 Bucket: ${bucketName}`);
console.log(`📝 Output: ${outputFile}`);
console.log("━".repeat(60));
console.log("");

// Helper: deslugify (reverse slugify)
function deslugify(slug) {
  return slug
    .split("-")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

// Helper: parse lecture filename
function parseLectureFilename(filename) {
  // Remove extension
  const nameWithoutExt = filename.replace(/\.(mp3|m4a|wav)$/i, "");

  // Try to extract lecture number (e.g., "01-title" or "1-title")
  const match = nameWithoutExt.match(/^(\d+)[-_\s]*(.*)/);

  if (match) {
    return {
      lectureNumber: parseInt(match[1], 10),
      title: deslugify(match[2] || nameWithoutExt),
    };
  }

  return {
    lectureNumber: 0,
    title: deslugify(nameWithoutExt),
  };
}

async function scanR2Bucket() {
  console.log("📡 Listing files from R2...");

  try {
    // List all objects in bucket
    const output = execSync(`wrangler r2 object list ${bucketName} --json`, {
      encoding: "utf8",
      maxBuffer: 10 * 1024 * 1024, // 10MB buffer
    });

    const objects = JSON.parse(output);
    console.log(`✅ Found ${objects.length} files\n`);

    // Parse R2 structure
    const speakers = {};

    for (const obj of objects) {
      const key = obj.key;

      // Expected structure: speakers/{speaker-slug}/...
      const parts = key.split("/");

      if (parts[0] !== "speakers" || parts.length < 2) {
        console.log(`⏭️  Skipping: ${key}`);
        continue;
      }

      const speakerSlug = parts[1];
      const speakerName = deslugify(speakerSlug);

      // Initialize speaker
      if (!speakers[speakerSlug]) {
        speakers[speakerSlug] = {
          name: speakerName,
          bio: null,
          imageUrl: null,
          imageSmallUrl: null,
          isPremium: false,
          collections: {},
        };
      }

      // Parse file type
      if (parts[2] === "images") {
        // Speaker image: speakers/{slug}/images/speaker.jpg
        const filename = parts[3];
        if (
          filename === "speaker.jpg" ||
          filename === "speaker.png" ||
          filename === "speaker.webp"
        ) {
          speakers[speakerSlug].imageUrl = `${speakerSlug}/${filename}`;
        } else if (filename.includes("small")) {
          speakers[speakerSlug].imageSmallUrl = `${speakerSlug}/${filename}`;
        }
      } else if (parts[2] === "collections" && parts.length >= 4) {
        // Collection: speakers/{slug}/collections/{collection-slug}/...
        const collectionSlug = parts[3];
        const collectionName = deslugify(collectionSlug);

        // Initialize collection
        if (!speakers[speakerSlug].collections[collectionSlug]) {
          speakers[speakerSlug].collections[collectionSlug] = {
            title: collectionName,
            description: null,
            year: null,
            coverImageUrl: null,
            coverImageSmallUrl: null,
            lectures: [],
          };
        }

        if (parts[4] === "images") {
          // Collection image: speakers/{slug}/collections/{coll}/images/cover.jpg
          const filename = parts[5];
          if (
            filename === "cover.jpg" ||
            filename === "cover.png" ||
            filename === "cover.webp"
          ) {
            speakers[speakerSlug].collections[
              collectionSlug
            ].coverImageUrl = `${speakerSlug}/${collectionSlug}/${filename}`;
          } else if (filename.includes("small")) {
            speakers[speakerSlug].collections[
              collectionSlug
            ].coverImageSmallUrl = `${speakerSlug}/${collectionSlug}/${filename}`;
          }
        } else if (parts[4] === "lectures") {
          // Lecture: speakers/{slug}/collections/{coll}/lectures/01-title.mp3
          const filename = parts[5];
          const { lectureNumber, title } = parseLectureFilename(filename);

          speakers[speakerSlug].collections[collectionSlug].lectures.push({
            title,
            description: null,
            lectureNumber,
            fileName: filename,
            filePath: key,
            duration: 0, // Unknown from R2
            fileSize: obj.size || 0,
            fileFormat: path.extname(filename).slice(1) || "mp3",
            bitrate: 0,
            sampleRate: 0,
            fileHash: null,
          });
        }
      }
    }

    // Convert to manifest format
    const manifest = {
      scannedAt: new Date().toISOString(),
      source: "r2-bucket-scan",
      speakers: [],
    };

    for (const [slug, speaker] of Object.entries(speakers)) {
      const speakerData = {
        name: speaker.name,
        bio: speaker.bio,
        imageUrl: speaker.imageUrl,
        imageSmallUrl: speaker.imageSmallUrl,
        isPremium: speaker.isPremium,
        collections: [],
      };

      for (const [collSlug, collection] of Object.entries(
        speaker.collections
      )) {
        // Sort lectures by number
        collection.lectures.sort((a, b) => a.lectureNumber - b.lectureNumber);
        speakerData.collections.push(collection);
      }

      manifest.speakers.push(speakerData);
    }

    // Sort speakers by name
    manifest.speakers.sort((a, b) => a.name.localeCompare(b.name));

    // Write manifest
    fs.writeFileSync(outputFile, JSON.stringify(manifest, null, 2));

    console.log("━".repeat(60));
    console.log("✅ Scan complete!");
    console.log("━".repeat(60));
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
    console.log("━".repeat(60));
    console.log(`📁 Saved to: ${outputFile}`);
    console.log("");
    console.log("⚠️  NEXT STEPS:");
    console.log(
      "   1. Review and edit the manifest file (add bios, descriptions, etc.)"
    );
    console.log("   2. Import to database:");
    console.log(
      `      DATABASE_URL="..." node import_manifest.js ${outputFile}`
    );
    console.log("");
  } catch (error) {
    console.error("\n❌ Error scanning R2:", error.message);
    console.error("");
    console.error("Make sure:");
    console.error("  1. Wrangler is installed: npm install -g wrangler");
    console.error("  2. You are authenticated: wrangler login");
    console.error("  3. Bucket name is correct");
    process.exit(1);
  }
}

// Run scan
scanR2Bucket();
