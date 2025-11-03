#!/bin/bash
# Quick PostgreSQL Schema Information Commands

DB_NAME="elmify_db"
HOST="localhost" 
PORT="5432"
USERNAME="alanhabib"

echo "🚀 Quick PostgreSQL Schema Information Commands"
echo "==============================================="

echo ""
echo "📋 1. List all tables with sizes:"
psql -h $HOST -p $PORT -U $USERNAME -d $DB_NAME -c "
SELECT 
    schemaname,
    tablename as table_name,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as total_size,
    pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) as table_size
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;"

echo ""
echo "🔗 2. Show all foreign key relationships:"
psql -h $HOST -p $PORT -U $USERNAME -d $DB_NAME -c "
SELECT
    tc.table_name,
    kcu.column_name,
    ccu.table_name AS references_table,
    ccu.column_name AS references_column
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_schema = 'public'
ORDER BY tc.table_name;"

echo ""
echo "📊 3. Column information for all tables:"
psql -h $HOST -p $PORT -U $USERNAME -d $DB_NAME -c "
SELECT 
    table_name,
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_schema = 'public' 
ORDER BY table_name, ordinal_position;"

echo ""
echo "🔍 4. Indexes and constraints:"
psql -h $HOST -p $PORT -U $USERNAME -d $DB_NAME -c "
SELECT 
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes 
WHERE schemaname = 'public'
ORDER BY tablename, indexname;"

echo ""
echo "📈 5. Table row counts (estimated):"
psql -h $HOST -p $PORT -U $USERNAME -d $DB_NAME -c "
SELECT 
    schemaname,
    tablename,
    n_tup_ins as inserts,
    n_tup_upd as updates,
    n_tup_del as deletes,
    n_live_tup as live_rows,
    n_dead_tup as dead_rows
FROM pg_stat_user_tables
ORDER BY n_live_tup DESC;"

echo ""
echo "✅ Schema information complete!"
echo ""
echo "💡 For more detailed analysis, run:"
echo "   psql -d $DB_NAME -f complete_schema_query.sql"
echo "   psql -d $DB_NAME -f get_schema_info.sql"
echo ""
echo "💾 To export schema to files, run:"
echo "   ./export_schema.sh"