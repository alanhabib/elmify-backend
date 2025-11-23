#!/usr/bin/env node

/**
 * Download content from Cloudflare R2 to local directory
 *
 * This script downloads files from R2 storage based on a manifest file.
 * Useful for backup, migration, or local development.
 *
 * Usage:
 *   # Download using exported manifest
 *   node download_from_r2.js exported_manifest.json ./restored_content
 *
 *   # Download specific speaker
 *   node download_from_r2.js exported_manifest.json ./content --speaker "Nouman Ali Khan"
 */

const fs = require("fs");
const path = require("path");
const { execSync } = require("child_process");

// Parse arguments
const manifestFile = process.argv[2];
const outputDir = process.argv[3] || "./downloaded_content";
const speakerFilter = process.argv.includes("--speaker")
  ? process.argv[process.argv.indexOf("--speaker") + 1]
  : null;

const bucketName = process.env.R2_BUCKET_NAME || "elmify-audio";

if (!manifestFile) {
  console.error(
    'Usage: node download_from_r2.js <manifest_file> [output_dir] [--speaker "Name"]'
  );
  process.exit(1);
}

if (!fs.existsSync(manifestFile)) {
  console.error(`Error: Manifest file not found: ${manifestFile}`);
  process.exit(1);
}

const manifest = JSON.parse(fs.readFileSync(manifestFile, "utf8"));

console.log("📥 R2 Download Tool");
console.log("━".repeat(60));
console.log(`📁 Manifest: ${manifestFile}`);
console.log(`📂 Output:   ${outputDir}`);
console.log(`🪣 Bucket:   ${bucketName}`);
if (speakerFilter) {
  console.log(`🔍 Filter:   ${speakerFilter}`);
}
console.log("━".repeat(60));
console.log("");

// Create output directory
if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true });
}

// Helper: slugify name
function slugify(text) {
  return text
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

// Helper: download file from R2
function downloadFromR2(r2Path, localPath) {
  try {
    const dir = path.dirname(localPath);
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }

    console.log(`  ⬇️  ${r2Path}`);

    // Use wrangler to download
    const cmd = `wrangler r2 object get ${bucketName}/${r2Path} --file "${localPath}"`;
    execSync(cmd, { stdio: "pipe" });

    return true;
  } catch (error) {
    console.error(`  ❌ Failed: ${error.message}`);
    return false;
  }
}

// Process speakers
let downloadedFiles = 0;
let failedFiles = 0;

for (const speaker of manifest.speakers) {
  // Apply filter if specified
  if (speakerFilter && speaker.name !== speakerFilter) {
    continue;
  }

  const speakerSlug = slugify(speaker.name);
  const speakerDir = path.join(outputDir, speaker.name);

  console.log(`📢 ${speaker.name}`);

  // Create speaker directory
  if (!fs.existsSync(speakerDir)) {
    fs.mkdirSync(speakerDir, { recursive: true });
  }

  // Create speaker.json
  const speakerJson = {
    name: speaker.name,
    bio: speaker.bio,
    isPremium: speaker.isPremium,
  };
  fs.writeFileSync(
    path.join(speakerDir, "speaker.json"),
    JSON.stringify(speakerJson, null, 2)
  );

  // Download speaker images
  if (speaker.imageUrl) {
    const r2Path = `speakers/${speakerSlug}/images/${path.basename(
      speaker.imageUrl
    )}`;
    const localPath = path.join(speakerDir, path.basename(speaker.imageUrl));
    if (downloadFromR2(r2Path, localPath)) {
      downloadedFiles++;
    } else {
      failedFiles++;
    }
  }

  if (speaker.imageSmallUrl) {
    const r2Path = `speakers/${speakerSlug}/images/${path.basename(
      speaker.imageSmallUrl
    )}`;
    const localPath = path.join(
      speakerDir,
      path.basename(speaker.imageSmallUrl)
    );
    if (downloadFromR2(r2Path, localPath)) {
      downloadedFiles++;
    } else {
      failedFiles++;
    }
  }

  // Process collections
  for (const collection of speaker.collections || []) {
    const collectionSlug = slugify(collection.title);
    const collectionDir = path.join(speakerDir, collection.title);

    console.log(`  📚 ${collection.title}`);

    if (!fs.existsSync(collectionDir)) {
      fs.mkdirSync(collectionDir, { recursive: true });
    }

    // Create info.json
    const collectionJson = {
      title: collection.title,
      description: collection.description,
      year: collection.year,
    };
    fs.writeFileSync(
      path.join(collectionDir, "info.json"),
      JSON.stringify(collectionJson, null, 2)
    );

    // Download collection images
    if (collection.coverImageUrl) {
      const r2Path = `speakers/${speakerSlug}/collections/${collectionSlug}/images/${path.basename(
        collection.coverImageUrl
      )}`;
      const localPath = path.join(
        collectionDir,
        path.basename(collection.coverImageUrl)
      );
      if (downloadFromR2(r2Path, localPath)) {
        downloadedFiles++;
      } else {
        failedFiles++;
      }
    }

    if (collection.coverImageSmallUrl) {
      const r2Path = `speakers/${speakerSlug}/collections/${collectionSlug}/images/${path.basename(
        collection.coverImageSmallUrl
      )}`;
      const localPath = path.join(
        collectionDir,
        path.basename(collection.coverImageSmallUrl)
      );
      if (downloadFromR2(r2Path, localPath)) {
        downloadedFiles++;
      } else {
        failedFiles++;
      }
    }

    // Download lectures
    for (const lecture of collection.lectures || []) {
      const fileName =
        lecture.fileName ||
        `${lecture.lectureNumber.toString().padStart(2, "0")}-${slugify(
          lecture.title
        )}.mp3`;
      const r2Path = `speakers/${speakerSlug}/collections/${collectionSlug}/lectures/${fileName}`;
      const localPath = path.join(collectionDir, fileName);

      if (downloadFromR2(r2Path, localPath)) {
        downloadedFiles++;
      } else {
        failedFiles++;
      }
    }
  }

  console.log("");
}

console.log("━".repeat(60));
console.log("✅ Download complete!");
console.log("━".repeat(60));
console.log(`📊 Summary:`);
console.log(`   Downloaded: ${downloadedFiles} files`);
console.log(`   Failed:     ${failedFiles} files`);
console.log(`   Location:   ${path.resolve(outputDir)}`);
console.log("━".repeat(60));

if (failedFiles > 0) {
  console.log("");
  console.log("⚠️  Some files failed to download. This may be normal if:");
  console.log("   - Files were deleted from R2");
  console.log("   - File paths changed");
  console.log("   - Metadata is out of sync");
}
