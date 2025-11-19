#!/usr/bin/env node

/**
 * Fix missing speaker_id in lectures table
 *
 * This script updates all lectures that have a collection_id but no speaker_id
 * by setting speaker_id to match the speaker of their collection.
 *
 * Usage:
 *   DATABASE_URL="postgresql://..." node fix_missing_speaker_ids.js
 */

const { Client } = require('pg');

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

async function fixSpeakerIds() {
  const client = new Client(DB_CONFIG);

  try {
    await client.connect();
    console.log('✅ Connected to database\n');

    // Step 1: Check how many lectures are missing speaker_id
    const countResult = await client.query(`
      SELECT COUNT(*) as missing_count 
      FROM lectures 
      WHERE speaker_id IS NULL AND collection_id IS NOT NULL
    `);

    const missingCount = parseInt(countResult.rows[0].missing_count);
    console.log(`📊 Found ${missingCount} lectures missing speaker_id\n`);

    if (missingCount === 0) {
      console.log('✅ All lectures already have speaker_id set!');
      return;
    }

    // Step 2: Update lectures to set speaker_id from their collection's speaker_id
    console.log('🔧 Updating lectures...');

    const updateResult = await client.query(`
      UPDATE lectures l
      SET speaker_id = c.speaker_id
      FROM collections c
      WHERE l.collection_id = c.id
        AND l.speaker_id IS NULL
        AND c.speaker_id IS NOT NULL
    `);

    console.log(`✅ Updated ${updateResult.rowCount} lectures\n`);

    // Step 3: Verify the fix
    const verifyResult = await client.query(`
      SELECT COUNT(*) as still_missing 
      FROM lectures 
      WHERE speaker_id IS NULL AND collection_id IS NOT NULL
    `);

    const stillMissing = parseInt(verifyResult.rows[0].still_missing);

    if (stillMissing === 0) {
      console.log('🎉 All lectures now have speaker_id!');
    } else {
      console.warn(`⚠️  ${stillMissing} lectures still missing speaker_id`);
      console.warn('   This may indicate orphaned lectures without valid collections.');
    }

    // Step 4: Show summary statistics
    console.log('\n' + '='.repeat(50));
    console.log('📊 Summary Statistics:');
    console.log('='.repeat(50));

    const statsResult = await client.query(`
      SELECT 
        s.name as speaker_name,
        COUNT(DISTINCT c.id) as collection_count,
        COUNT(l.id) as lecture_count
      FROM speakers s
      LEFT JOIN collections c ON s.id = c.speaker_id
      LEFT JOIN lectures l ON c.id = l.collection_id AND l.speaker_id = s.id
      GROUP BY s.id, s.name
      ORDER BY s.name
    `);

    statsResult.rows.forEach(row => {
      console.log(`  ${row.speaker_name}: ${row.collection_count} collections, ${row.lecture_count} lectures`);
    });

    console.log('='.repeat(50) + '\n');

  } catch (error) {
    console.error('❌ Error:', error.message);
    console.error(error.stack);
    process.exit(1);
  } finally {
    await client.end();
  }
}

// Run the fix
fixSpeakerIds();

