#!/usr/bin/env node

/**
 * Assign categories to existing collections based on mapping rules.
 *
 * Usage:
 *   node assign_categories.js                    # Run assignment
 *   node assign_categories.js --list-categories  # Show available categories
 *   node assign_categories.js --list-collections # Show uncategorized collections
 *   node assign_categories.js --dry-run          # Preview without changes
 *
 * Edit category_mappings.json to define your mappings.
 */

const fs = require('fs');
const path = require('path');
const { Client } = require('pg');
require('dotenv').config({ path: path.join(__dirname, '..', '.env') });

// Database connection config
const DB_CONFIG = process.env.DATABASE_URL ? {
  connectionString: process.env.DATABASE_URL,
  ssl: { rejectUnauthorized: false }
} : {
  host: 'localhost',
  port: 5432,
  database: 'elmify_db',
  user: 'alanhabib',
  password: 'password'
};

const args = process.argv.slice(2);
const isDryRun = args.includes('--dry-run');
const listCategories = args.includes('--list-categories');
const listCollections = args.includes('--list-collections');

async function main() {
  const client = new Client(DB_CONFIG);

  try {
    await client.connect();
    console.log('✅ Connected to database\n');

    // Load all categories from database
    const categoriesResult = await client.query(`
      SELECT id, slug, name, parent_id
      FROM categories
      ORDER BY parent_id NULLS FIRST, display_order
    `);

    const categories = categoriesResult.rows;
    const categoryBySlug = new Map(categories.map(c => [c.slug, c]));
    const validSlugs = new Set(categories.map(c => c.slug));

    // List categories mode
    if (listCategories) {
      console.log('📋 Available Categories:\n');
      console.log('Top-level categories:');
      categories.filter(c => !c.parent_id).forEach(c => {
        console.log(`  • ${c.slug} - "${c.name}"`);
      });
      console.log('\nSubcategories:');
      categories.filter(c => c.parent_id).forEach(c => {
        const parent = categories.find(p => p.id === c.parent_id);
        console.log(`  • ${c.slug} - "${c.name}" (under ${parent?.name})`);
      });
      return;
    }

    // List uncategorized collections mode
    if (listCollections) {
      const uncategorized = await client.query(`
        SELECT c.id, c.title, s.name as speaker_name
        FROM collections c
        JOIN speakers s ON c.speaker_id = s.id
        WHERE NOT EXISTS (
          SELECT 1 FROM collection_categories cc WHERE cc.collection_id = c.id
        )
        ORDER BY s.name, c.title
      `);

      console.log(`📋 Uncategorized Collections (${uncategorized.rows.length}):\n`);
      let currentSpeaker = '';
      uncategorized.rows.forEach(c => {
        if (c.speaker_name !== currentSpeaker) {
          currentSpeaker = c.speaker_name;
          console.log(`\n${currentSpeaker}:`);
        }
        console.log(`  • [${c.id}] ${c.title}`);
      });
      return;
    }

    // Load mappings file
    const mappingsFile = path.join(__dirname, 'category_mappings.json');
    if (!fs.existsSync(mappingsFile)) {
      console.log('📝 Creating template category_mappings.json...\n');

      // Get all speakers and collections
      const speakersResult = await client.query(`
        SELECT s.id, s.name,
               json_agg(json_build_object('id', c.id, 'title', c.title) ORDER BY c.title) as collections
        FROM speakers s
        LEFT JOIN collections c ON c.speaker_id = s.id
        GROUP BY s.id, s.name
        ORDER BY s.name
      `);

      const template = {
        "_instructions": "Map speakers/collections to category slugs. Run with --list-categories to see valid slugs.",
        "_validSlugs": Array.from(validSlugs).sort(),
        "speakerDefaults": {},
        "collectionOverrides": {}
      };

      // Add all speakers with null defaults
      speakersResult.rows.forEach(speaker => {
        template.speakerDefaults[speaker.name] = null;

        // Add collection overrides template
        if (speaker.collections && speaker.collections[0]) {
          speaker.collections.forEach(col => {
            if (col.title) {
              template.collectionOverrides[`${speaker.name}/${col.title}`] = null;
            }
          });
        }
      });

      fs.writeFileSync(mappingsFile, JSON.stringify(template, null, 2));
      console.log(`✅ Created ${mappingsFile}`);
      console.log('\nEdit this file to assign categories, then run the script again.');
      console.log('\nExample mappings:');
      console.log('  "speakerDefaults": {');
      console.log('    "Abdul Rashid Sufi": "quran-recitation",');
      console.log('    "Anwar Awlaki": "islamic-history"');
      console.log('  },');
      console.log('  "collectionOverrides": {');
      console.log('    "Anwar Awlaki/Hereafter Series": "spirituality"');
      console.log('  }');
      return;
    }

    // Load and validate mappings
    const mappings = JSON.parse(fs.readFileSync(mappingsFile, 'utf8'));

    // Validate all slugs in mappings
    const invalidSlugs = [];
    Object.entries(mappings.speakerDefaults || {}).forEach(([speaker, slug]) => {
      if (slug && !validSlugs.has(slug)) {
        invalidSlugs.push(`speakerDefaults["${speaker}"] = "${slug}"`);
      }
    });
    Object.entries(mappings.collectionOverrides || {}).forEach(([key, slug]) => {
      if (slug && !validSlugs.has(slug)) {
        invalidSlugs.push(`collectionOverrides["${key}"] = "${slug}"`);
      }
    });

    if (invalidSlugs.length > 0) {
      console.error('❌ Invalid category slugs found:\n');
      invalidSlugs.forEach(s => console.error(`  • ${s}`));
      console.error('\nRun with --list-categories to see valid slugs.');
      process.exit(1);
    }

    // Get all collections with speaker info
    const collectionsResult = await client.query(`
      SELECT c.id, c.title, s.name as speaker_name
      FROM collections c
      JOIN speakers s ON c.speaker_id = s.id
      ORDER BY s.name, c.title
    `);

    if (isDryRun) {
      console.log('🔍 DRY RUN - No changes will be made\n');
    }

    let assigned = 0;
    let skipped = 0;

    for (const collection of collectionsResult.rows) {
      const key = `${collection.speaker_name}/${collection.title}`;

      // Check for collection-specific override first, then speaker default
      let categorySlug = mappings.collectionOverrides?.[key]
        || mappings.speakerDefaults?.[collection.speaker_name]
        || null;

      if (!categorySlug) {
        skipped++;
        continue;
      }

      const category = categoryBySlug.get(categorySlug);
      if (!category) {
        console.log(`⚠️  Category not found: ${categorySlug} for "${key}"`);
        continue;
      }

      // Check if already assigned
      const existing = await client.query(
        `SELECT 1 FROM collection_categories WHERE collection_id = $1 AND category_id = $2`,
        [collection.id, category.id]
      );

      if (existing.rows.length > 0) {
        continue; // Already assigned
      }

      console.log(`📁 ${key} → ${categorySlug}`);

      if (!isDryRun) {
        // Assign to collection
        await client.query(
          `INSERT INTO collection_categories (collection_id, category_id, is_primary, created_at)
           VALUES ($1, $2, true, NOW())
           ON CONFLICT (collection_id, category_id) DO NOTHING`,
          [collection.id, category.id]
        );

        // Cascade to all lectures in collection
        await client.query(
          `INSERT INTO lecture_categories (lecture_id, category_id, is_primary, created_at)
           SELECT l.id, $1, true, NOW()
           FROM lectures l
           WHERE l.collection_id = $2
           ON CONFLICT (lecture_id, category_id) DO NOTHING`,
          [category.id, collection.id]
        );
      }

      assigned++;
    }

    console.log('\n' + '='.repeat(50));
    console.log(isDryRun ? '🔍 DRY RUN COMPLETE' : '✅ Assignment complete!');
    console.log('='.repeat(50));
    console.log(`Collections assigned: ${assigned}`);
    console.log(`Collections skipped (no mapping): ${skipped}`);
    console.log('='.repeat(50));

  } catch (error) {
    console.error('\n❌ Error:', error.message);
    console.error(error.stack);
    process.exit(1);
  } finally {
    await client.end();
  }
}

main();
