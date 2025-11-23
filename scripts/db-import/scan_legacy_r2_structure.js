#!/usr/bin/env node

/**
 * Scan LEGACY R2 bucket structure and generate metadata
 *
 * This handles the OLD structure where files are organized as:
 *   Speaker Name/
 *     Collection Name/
 *       lecture.mp3
 *       collection.jpg
 *     speaker.jpg
 *
 * Instead of the NEW structure:
 *   speakers/speaker-slug/
 *     images/speaker.jpg
 *     collections/collection-slug/
 *       images/cover.jpg
 *       lectures/lecture.mp3
 *
 * Usage:
 *   node scan_legacy_r2_structure.js [bucket_name]
 */

const { execSync } = require("child_process");
const fs = require("fs");
const path = require("path");

const bucketName =
  process.argv[2] || process.env.R2_BUCKET_NAME || "elmify-audio";
const outputFile = "r2_legacy_manifest.json";

console.log("🔍 Legacy R2 Structure Scanner");
console.log("━".repeat(60));
console.log(`🪣 Bucket: ${bucketName}`);
console.log(`📝 Output: ${outputFile}`);
console.log("━".repeat(60));
console.log("");

// Helper: slugify
function slugify(text) {
  return text
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

// Helper: parse lecture filename
function parseLectureFilename(filename) {
  // Remove extension
  const nameWithoutExt = filename.replace(/\.(mp3|m4a|wav)$/i, "");

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

// Helper: determine file type
function getFileType(filename) {
  const lower = filename.toLowerCase();

  if (lower.match(/\.(mp3|m4a|wav|aac|flac)$/)) return "audio";
  if (lower.match(/\.(jpg|jpeg|png|webp|gif)$/)) return "image";
  if (lower.match(/\.json$/)) return "json";

  return "unknown";
}

async function scanR2Bucket() {
  console.log("📡 Listing files from R2...");

  try {
    // List all objects in bucket
    const output = execSync(`wrangler r2 object list ${bucketName} --json`, {
      encoding: "utf8",
      maxBuffer: 50 * 1024 * 1024, // 50MB buffer
    });

    const objects = JSON.parse(output);
    console.log(`✅ Found ${objects.length} files\n`);

    // Parse structure
    const speakers = {};

    for (const obj of objects) {
      const key = obj.key;
      const parts = key.split("/");

      if (parts.length < 2) {
        console.log(`⏭️  Skipping root file: ${key}`);
        continue;
      }

      const speakerName = parts[0];
      const speakerSlug = slugify(speakerName);

      // Initialize speaker
      if (!speakers[speakerSlug]) {
        speakers[speakerSlug] = {
          name: speakerName,
          slug: speakerSlug,
          bio: null,
          imageUrl: null,
          imageSmallUrl: null,
          isPremium: false,
          collections: {},
        };
      }

      // Speaker-level files
      if (parts.length === 2) {
        const filename = parts[1];
        const fileType = getFileType(filename);

        if (fileType === "json" && filename === "speaker.json") {
          // Will read JSON content later if needed
          speakers[speakerSlug].hasJson = true;
        } else if (fileType === "image") {
          if (filename.includes("small")) {
            speakers[speakerSlug].imageSmallUrl = key;
          } else {
            speakers[speakerSlug].imageUrl = key;
          }
        }
        continue;
      }

      // Collection-level files
      if (parts.length >= 3) {
        const collectionName = parts[1];
        const collectionSlug = slugify(collectionName);
        const filename = parts[2];
        const fileType = getFileType(filename);

        // Initialize collection
        if (!speakers[speakerSlug].collections[collectionSlug]) {
          speakers[speakerSlug].collections[collectionSlug] = {
            title: collectionName,
            slug: collectionSlug,
            description: null,
            year: null,
            coverImageUrl: null,
            coverImageSmallUrl: null,
            lectures: [],
            lectureJsons: [],
          };
        }

        const collection = speakers[speakerSlug].collections[collectionSlug];

        if (fileType === "json") {
          if (filename === "collection.json") {
            collection.hasJson = true;
          } else {
            // Lecture-specific JSON (e.g., "01 - Introduction.json")
            collection.lectureJsons.push(filename);
          }
        } else if (fileType === "image") {
          if (filename.includes("small")) {
            collection.coverImageSmallUrl = key;
          } else if (filename.startsWith("collection")) {
            collection.coverImageUrl = key;
          }
        } else if (fileType === "audio") {
          const { lectureNumber, title } = parseLectureFilename(filename);

          collection.lectures.push({
            title,
            description: null,
            lectureNumber,
            fileName: filename,
            filePath: key,
            duration: 0,
            fileSize: obj.size || 0,
            fileFormat: path.extname(filename).slice(1).toLowerCase() || "mp3",
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
      source: "r2-legacy-structure-scan",
      structure: "flat (Speaker/Collection/file.mp3)",
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

        speakerData.collections.push({
          title: collection.title,
          description: collection.description,
          year: collection.year,
          coverImageUrl: collection.coverImageUrl,
          coverImageSmallUrl: collection.coverImageSmallUrl,
          lectures: collection.lectures,
        });
      }

      // Sort collections by title
      speakerData.collections.sort((a, b) => a.title.localeCompare(b.title));

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

    const totalCollections = manifest.speakers.reduce(
      (sum, s) => sum + s.collections.length,
      0
    );
    console.log(`   Collections: ${totalCollections}`);

    const totalLectures = manifest.speakers.reduce(
      (sum, s) =>
        sum + s.collections.reduce((cSum, c) => cSum + c.lectures.length, 0),
      0
    );
    console.log(`   Lectures:    ${totalLectures}`);

    console.log("━".repeat(60));
    console.log(`📁 Saved to: ${outputFile}`);
    console.log("");
    console.log("📋 Speakers found:");
    manifest.speakers.forEach((s) => {
      console.log(`   • ${s.name} (${s.collections.length} collections)`);
    });
    console.log("");
    console.log("⚠️  NEXT STEPS:");
    console.log(
      "   1. Review the manifest file (add bios, descriptions, etc.)"
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
