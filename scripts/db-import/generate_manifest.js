#!/usr/bin/env node

/**
 * Generate manifest.json from local content directory
 *
 * Usage: node generate_manifest.js <content_directory> [output_file]
 */

const fs = require('fs');
const path = require('path');

// Store object keys only, not full URLs
// Backend will generate presigned URLs dynamically
const STORE_KEYS_ONLY = true;

// Get command line arguments
const contentDir = process.argv[2];
const outputFile = process.argv[3] || 'manifest.json';

if (!contentDir) {
  console.error('Usage: node generate_manifest.js <content_directory> [output_file]');
  process.exit(1);
}

if (!fs.existsSync(contentDir)) {
  console.error(`Error: Directory not found: ${contentDir}`);
  process.exit(1);
}

console.log('🔄 Generating manifest from:', contentDir);
console.log('📝 Output file:', outputFile);
console.log('');

const manifest = {
  speakers: []
};

// Helper: URL encode path for R2
function encodeR2Path(localPath) {
  return localPath.split('/').map(encodeURIComponent).join('/');
}

// Process speaker directory
function processSpeaker(speakerDir) {
  const speakerName = path.basename(speakerDir);

  console.log(`📢 Processing speaker: ${speakerName}`);

  // Read speaker.json
  const speakerJsonPath = path.join(speakerDir, 'speaker.json');
  if (!fs.existsSync(speakerJsonPath)) {
    console.warn(`  ⚠️  Missing speaker.json, skipping...`);
    return null;
  }

  const speakerData = JSON.parse(fs.readFileSync(speakerJsonPath, 'utf8'));

  // Find speaker images
  let speakerImageFile = null;
  let speakerImageSmallFile = null;

  for (const ext of ['jpg', 'jpeg', 'png', 'webp']) {
    if (fs.existsSync(path.join(speakerDir, `speaker.${ext}`))) {
      speakerImageFile = `speaker.${ext}`;
    }
    if (fs.existsSync(path.join(speakerDir, `speaker_small.${ext}`))) {
      speakerImageSmallFile = `speaker_small.${ext}`;
    }
  }

  const speaker = {
    name: speakerData.name || speakerName,
    bio: speakerData.bio || null,
    imageUrl: speakerImageFile ? `${speakerName}/${speakerImageFile}` : null,
    imageSmallUrl: speakerImageSmallFile ? `${speakerName}/${speakerImageSmallFile}` : null,
    isPremium: speakerData.isPremium || false,
    collections: []
  };

  // Process collections
  const items = fs.readdirSync(speakerDir);
  for (const item of items) {
    const itemPath = path.join(speakerDir, item);
    if (!fs.statSync(itemPath).isDirectory()) continue;

    const collection = processCollection(itemPath, speakerName);
    if (collection) {
      speaker.collections.push(collection);
    }
  }

  console.log(`  ✅ Found ${speaker.collections.length} collections`);

  return speaker;
}

// Process collection directory
function processCollection(collectionDir, speakerName) {
  const collectionName = path.basename(collectionDir);

  console.log(`  📚 Processing collection: ${collectionName}`);

  // Read collection.json if exists
  let collectionData = {};
  const collectionJsonPath = path.join(collectionDir, 'collection.json');
  if (fs.existsSync(collectionJsonPath)) {
    collectionData = JSON.parse(fs.readFileSync(collectionJsonPath, 'utf8'));
  }

  // Find collection images
  let coverImageFile = null;
  let coverImageSmallFile = null;

  for (const ext of ['jpg', 'jpeg', 'png', 'webp']) {
    if (fs.existsSync(path.join(collectionDir, `collection.${ext}`))) {
      coverImageFile = `collection.${ext}`;
    }
    if (fs.existsSync(path.join(collectionDir, `collection_small.${ext}`))) {
      coverImageSmallFile = `collection_small.${ext}`;
    }
  }

  const collection = {
    title: collectionData.title || collectionName,
    description: collectionData.description || null,
    year: collectionData.year || null,
    coverImageUrl: coverImageFile ? `${speakerName}/${collectionName}/${coverImageFile}` : null,
    coverImageSmallUrl: coverImageSmallFile ? `${speakerName}/${collectionName}/${coverImageSmallFile}` : null,
    lectures: []
  };

  // Process lectures (audio files)
  const files = fs.readdirSync(collectionDir);
  const audioFiles = files.filter(f => /\.(mp3|m4a|wav|flac|aac|ogg)$/i.test(f));
  audioFiles.sort(); // Sort by filename

  for (const audioFile of audioFiles) {
    const lecture = processLecture(audioFile, speakerName, collectionName);
    if (lecture) {
      collection.lectures.push(lecture);
    }
  }

  console.log(`    ✅ Found ${collection.lectures.length} lectures`);

  return collection;
}

// Process lecture (audio file)
function processLecture(filename, speakerName, collectionName) {
  // Extract title from filename: "01 - Title.mp3" -> "Title"
  const match = filename.match(/^(\d{2,3})\s*-\s*(.+)\.(mp3|m4a|wav|flac|aac|ogg)$/i);

  let title = filename.replace(/\.(mp3|m4a|wav|flac|aac|ogg)$/i, '');
  let trackNumber = null;
  let fileFormat = 'mp3';

  if (match) {
    trackNumber = parseInt(match[1], 10);
    title = match[2];
    fileFormat = match[3].toLowerCase();
  }

  const filePath = `${speakerName}/${collectionName}/${filename}`;

  return {
    title: title,
    lectureNumber: trackNumber,
    fileName: filename,
    filePath: filePath,
    fileFormat: fileFormat,
    duration: null,
    fileSize: null,
    bitrate: null,
    sampleRate: null,
    fileHash: null
  };
}

// Main processing
try {
  const speakerDirs = fs.readdirSync(contentDir);

  for (const item of speakerDirs) {
    const itemPath = path.join(contentDir, item);

    // Skip hidden files and non-directories
    if (item.startsWith('.') || !fs.statSync(itemPath).isDirectory()) {
      continue;
    }

    const speaker = processSpeaker(itemPath);
    if (speaker) {
      manifest.speakers.push(speaker);
    }
  }

  // Write manifest
  fs.writeFileSync(outputFile, JSON.stringify(manifest, null, 2));

  console.log('');
  console.log('✅ Manifest generated successfully!');
  console.log('');
  console.log('📊 Summary:');
  console.log(`   Speakers: ${manifest.speakers.length}`);
  console.log(`   Collections: ${manifest.speakers.reduce((sum, s) => sum + s.collections.length, 0)}`);
  console.log(`   Lectures: ${manifest.speakers.reduce((sum, s) => sum + s.collections.reduce((cSum, c) => cSum + c.lectures.length, 0), 0)}`);
  console.log('');
  console.log(`📄 Manifest saved to: ${outputFile}`);

} catch (error) {
  console.error('❌ Error:', error.message);
  process.exit(1);
}
