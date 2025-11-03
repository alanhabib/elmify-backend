#!/bin/bash
# Complete PostgreSQL Schema Export Script
# Usage: ./export_schema.sh

DB_NAME="elmify_db"
HOST="localhost"
PORT="5432"
USERNAME="alanhabib"

echo "🗄️  Exporting PostgreSQL schema for $DB_NAME..."

# Method 1: Schema only (structure, no data)
echo "📋 Exporting schema structure only..."
pg_dump -h $HOST -p $PORT -U $USERNAME -d $DB_NAME \
    --schema-only \
    --no-owner \
    --no-privileges \
    --verbose \
    -f schema_structure.sql

# Method 2: Schema with constraints and relationships
echo "🔗 Exporting schema with detailed constraints..."
pg_dump -h $HOST -p $PORT -U $USERNAME -d $DB_NAME \
    --schema-only \
    --no-owner \
    --no-privileges \
    --create \
    --verbose \
    -f schema_with_constraints.sql

# Method 3: Custom format (can be restored with pg_restore)
echo "📦 Exporting in custom format..."
pg_dump -h $HOST -p $PORT -U $USERNAME -d $DB_NAME \
    --schema-only \
    --format=custom \
    --no-owner \
    --no-privileges \
    --verbose \
    -f schema_backup.dump

# Method 4: Human-readable schema information
echo "📖 Generating human-readable schema report..."
psql -h $HOST -p $PORT -U $USERNAME -d $DB_NAME \
    -c "
    SELECT 
        'TABLE: ' || table_name as info,
        '  Type: ' || table_type as details
    FROM information_schema.tables 
    WHERE table_schema = 'public' 
    ORDER BY table_name;
    
    SELECT 
        'COLUMN: ' || table_name || '.' || column_name as info,
        '  ' || data_type || 
        CASE 
            WHEN character_maximum_length IS NOT NULL 
            THEN '(' || character_maximum_length || ')'
            ELSE ''
        END ||
        CASE WHEN is_nullable = 'NO' THEN ' NOT NULL' ELSE '' END ||
        CASE WHEN column_default IS NOT NULL THEN ' DEFAULT ' || column_default ELSE '' END
        as details
    FROM information_schema.columns 
    WHERE table_schema = 'public' 
    ORDER BY table_name, ordinal_position;
    " > schema_readable.txt

echo "✅ Schema export complete! Files created:"
echo "   📄 schema_structure.sql - Basic structure"
echo "   📄 schema_with_constraints.sql - With constraints"
echo "   📦 schema_backup.dump - Custom format"
echo "   📖 schema_readable.txt - Human readable"
echo ""
echo "To view schema interactively:"
echo "   psql -d $DB_NAME -f get_schema_info.sql"