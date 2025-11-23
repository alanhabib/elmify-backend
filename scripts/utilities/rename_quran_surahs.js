#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

// List of 114 Surahs
const surahs = [
  "Al-Fatiha (The opener)",
  "Al-Baqarah (The cow)",
  "Al-Imran (Family of Imran)",
  "An-Nisa (The Women)",
  "Al-Ma'idah (The Table Spread)",
  "Al-Anam (The Cattle)",
  "Al-A'raf (The Heights)",
  "Al-Anfal (The Spoils of War)",
  "At-Taubah (The Repentance)",
  "Yunus (Jonah)",
  "Hud (Hud)",
  "Yusuf (Joseph)",
  "Ar-Ra'd (Thunder)",
  "Ibrahim (Abraham)",
  "Al-Hijr (The Stoneland)",
  "An-Nahl (The Bee)",
  "Al-Isra (The Night Journey)",
  "Al-Kahf (The Cave)",
  "Maryam (Mary)",
  "Ta-Ha (Ta-Ha)",
  "Al-Anbiya (The Prophets)",
  "Al-Hajj (The Pilgrimage)",
  "Al-Mu'minun (The Believers)",
  "An-Nur (The Light)",
  "Al-Furqan (The Criterion)",
  "Ash-Shu'ara (The Poets)",
  "An-Naml (The Ants)",
  "Al-Qasas (The Story)",
  "Al-Ankabut (Spider)",
  "Ar-Rum (The Romans)",
  "Luqman (Luqman)",
  "As-Sajdah (Prostration)",
  "Al-Ahzab (The Confederates)",
  "Saba (Sheba)",
  "Fatir (The Originator)",
  "Ya-Sin (Ya Sin)",
  "As-Saffat (Those Who Set the Ranks)",
  "Sad (The letter Saad)",
  "Az-Zumar (The Troops)",
  "Ghafir (The Forgiver)",
  "Fussilat (Explained in Detail)",
  "Ash-Shura (The Consultation)",
  "Az-Zukhruf (The Ornaments of Gold)",
  "Ad-Dukhan (The Smoke)",
  "Al-Jathiyah (The Crouching)",
  "Al-Ahqaf (The Wind Curved Sandhill)",
  "Muhammad (Muhammad)",
  "Al-Fath (The Victory)",
  "Al-Hujurat (The Private Chambers)",
  "Qaf (Qaf)",
  "Adh-Dhariyat (The Scatterers)",
  "At-Tur (The Mountain)",
  "An-Najm (The Star)",
  "Al-Qamar (The Moon)",
  "Ar-Rahman (The Beneficent)",
  "Al-Waqi'ah (The Inevitable)",
  "Al-Hadid (The Iron)",
  "Al-Mujadila (The Pleading Women)",
  "Al-Hashr (The Exile)",
  "Al-Mumtahanah (She That is to be Examined)",
  "As-Saff (The Ranks)",
  "Al-Jumu'ah (Congregation Prayer)",
  "Al-Munafiqun (The Hypocrites)",
  "At-Taghabun (Mutual Disposession)",
  "At-Talaq (The Divorce)",
  "At-Tahrim (The Prohibition)",
  "Al-Mulk (The Sovereignty)",
  "Al-Qalam (The Pen)",
  "Al-Haqqah (The Reality)",
  "Al-Ma'arij (The Ascending Stairways)",
  "Nuh (Noah)",
  "Al-Jinn (The Jinn)",
  "Al-Muzzammil (The Enshrouded One)",
  "Al-Muddaththir (The Cloaked One)",
  "Al-Qiyamah (The Resurrection)",
  "Al-Insan (The Man)",
  "Al-Mursalat (The Emissaries)",
  "An-Naba (The Tidings)",
  "An-Nazi'at (Those who drag forth)",
  "Abasa (He Frowned)",
  "At-Takwir (The Overthrowing)",
  "Al-Infitar (The Cleaving)",
  "Al-Mutaffifin (The Defrauding)",
  "Al-Inshiqaq (The Sundering)",
  "Al-Buruj (The Mansions of the Stars)",
  "At-Tariq (The Nightcommer)",
  "Al-Ala (The Most High)",
  "Al-Ghashiyah (The Overwhelming)",
  "Al-Fajr (The Dawn)",
  "Al-Balad (The City)",
  "Ash-Shams (The Sun)",
  "Al-Lail (The Night)",
  "Ad-Duha (The Morning Brightness)",
  "Ash-Sharh (The Expansion)",
  "At-Tin (The Fig)",
  "Al-Alaq (The Blood Clot)",
  "Al-Qadr (The Power)",
  "Al-Bayyina (The Evidence)",
  "Az-Zalzalah (The Earthquake)",
  "Al-Adiyat (The Courser)",
  "Al-Qari'ah (The Calamity)",
  "At-Takathur (Vying for increase)",
  "Al-Asr (The Declining Day)",
  "Al-Humazah (The Slanderer)",
  "Al-Fil (The Elephant)",
  "Quraysh (Quraish)",
  "Al-Ma'un (The Small Kindness)",
  "Al-Kawthar (The Abundance)",
  "Al-Kafirun (The Disbelievers)",
  "An-Nasr (The Divine Support)",
  "Al-Masad (The Palm Fiber)",
  "Al-Ikhlas (The Sincerity)",
  "Al-Falaq (The Daybreak)",
  "An-Nas (The Mankind)"
];

function renameFiles(folderPath, pattern) {
  console.log(`\n📂 Processing folder: ${folderPath}`);

  if (!fs.existsSync(folderPath)) {
    console.error(`❌ Folder does not exist: ${folderPath}`);
    return;
  }

  const files = fs.readdirSync(folderPath);
  let renamed = 0;
  let errors = 0;

  for (let i = 1; i <= 114; i++) {
    // Format: 001.mp3 or 001_2.mp3
    const paddedNumber = String(i).padStart(3, '0');
    const oldFilename = pattern.replace('{num}', paddedNumber);
    const oldPath = path.join(folderPath, oldFilename);

    if (!fs.existsSync(oldPath)) {
      console.log(`⚠️  File not found: ${oldFilename}`);
      continue;
    }

    // New format: 01 - Surah Name.mp3 or 114 - Surah Name.mp3
    const displayNumber = i < 10 ? `0${i}` : String(i);
    const newFilename = `${displayNumber} - ${surahs[i - 1]}.mp3`;
    const newPath = path.join(folderPath, newFilename);

    try {
      fs.renameSync(oldPath, newPath);
      console.log(`✅ ${oldFilename} → ${newFilename}`);
      renamed++;
    } catch (error) {
      console.error(`❌ Error renaming ${oldFilename}: ${error.message}`);
      errors++;
    }
  }

  console.log(`\n📊 Summary for ${folderPath}:`);
  console.log(`   ✅ Renamed: ${renamed} files`);
  console.log(`   ❌ Errors: ${errors} files`);
}

// Get command line arguments
const args = process.argv.slice(2);

console.log('🔄 Starting Quran Surah Renaming Process...\n');

if (args.length === 0) {
  // Default behavior: process both original folders
  const folder1 = '/Users/alanhabib/Desktop/hobby_projects/batch/test_upload/maher al-muaiqly/Quran Hafs';
  const folder2 = '/Users/alanhabib/Desktop/hobby_projects/batch/test_upload/Abdul rashid sufi/Quran Hafs';

  renameFiles(folder1, '{num}.mp3');
  renameFiles(folder2, '{num}_2.mp3');
} else {
  // Process folders passed as arguments
  // Usage: node rename_quran_surahs.js "/path/to/folder" "{num}.mp3"
  // Or:    node rename_quran_surahs.js "/path/to/folder" (defaults to {num}.mp3)

  const folderPath = args[0];
  const pattern = args[1] || '{num}.mp3';

  renameFiles(folderPath, pattern);
}

console.log('\n✨ Renaming process complete!\n');
