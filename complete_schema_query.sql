-- COMPREHENSIVE DATABASE SCHEMA INFORMATION
-- Run with: psql -d elmify_db -f complete_schema_query.sql

-- =====================================================
-- COMPLETE TABLE AND COLUMN INFORMATION IN ONE QUERY
-- =====================================================

WITH table_info AS (
    SELECT 
        t.table_name,
        t.table_type,
        obj_description(c.oid, 'pg_class') as table_comment,
        pg_size_pretty(pg_total_relation_size(c.oid)) as table_size,
        pg_stat_get_tuples_returned(c.oid) as row_count_estimate
    FROM information_schema.tables t
    LEFT JOIN pg_class c ON c.relname = t.table_name
    WHERE t.table_schema = 'public'
        AND t.table_type = 'BASE TABLE'
),
column_info AS (
    SELECT 
        c.table_name,
        c.column_name,
        c.ordinal_position,
        c.data_type,
        c.character_maximum_length,
        c.numeric_precision,
        c.numeric_scale,
        c.is_nullable,
        c.column_default,
        col_description(pgc.oid, c.ordinal_position) as column_comment
    FROM information_schema.columns c
    LEFT JOIN pg_class pgc ON pgc.relname = c.table_name
    WHERE c.table_schema = 'public'
),
constraint_info AS (
    SELECT 
        tc.table_name,
        tc.constraint_name,
        tc.constraint_type,
        kcu.column_name,
        ccu.table_name AS foreign_table_name,
        ccu.column_name AS foreign_column_name
    FROM information_schema.table_constraints tc
    LEFT JOIN information_schema.key_column_usage kcu 
        ON tc.constraint_name = kcu.constraint_name
    LEFT JOIN information_schema.constraint_column_usage ccu 
        ON ccu.constraint_name = tc.constraint_name
    WHERE tc.table_schema = 'public'
),
index_info AS (
    SELECT 
        schemaname,
        tablename,
        indexname,
        indexdef
    FROM pg_indexes 
    WHERE schemaname = 'public'
)

-- MAIN COMPREHENSIVE QUERY
SELECT 
    ti.table_name,
    ti.table_type,
    ti.table_comment,
    ti.table_size,
    ti.row_count_estimate,
    ci.column_name,
    ci.ordinal_position,
    ci.data_type,
    CASE 
        WHEN ci.character_maximum_length IS NOT NULL 
        THEN ci.data_type || '(' || ci.character_maximum_length || ')'
        WHEN ci.numeric_precision IS NOT NULL AND ci.numeric_scale IS NOT NULL
        THEN ci.data_type || '(' || ci.numeric_precision || ',' || ci.numeric_scale || ')'
        WHEN ci.numeric_precision IS NOT NULL 
        THEN ci.data_type || '(' || ci.numeric_precision || ')'
        ELSE ci.data_type
    END as full_data_type,
    ci.is_nullable,
    ci.column_default,
    ci.column_comment,
    COALESCE(
        string_agg(
            CASE coni.constraint_type
                WHEN 'PRIMARY KEY' THEN 'PK'
                WHEN 'FOREIGN KEY' THEN 'FK -> ' || coni.foreign_table_name || '(' || coni.foreign_column_name || ')'
                WHEN 'UNIQUE' THEN 'UNIQUE'
                WHEN 'CHECK' THEN 'CHECK'
                ELSE coni.constraint_type
            END, ', '
        ), 
        ''
    ) as constraints,
    COALESCE(
        string_agg(ii.indexname, ', '), 
        ''
    ) as indexes
FROM table_info ti
LEFT JOIN column_info ci ON ti.table_name = ci.table_name
LEFT JOIN constraint_info coni ON ci.table_name = coni.table_name AND ci.column_name = coni.column_name
LEFT JOIN index_info ii ON ci.table_name = ii.tablename 
    AND (ii.indexdef LIKE '%' || ci.column_name || '%' OR ii.indexdef LIKE '%' || ci.column_name || ',%')
GROUP BY 
    ti.table_name, ti.table_type, ti.table_comment, ti.table_size, ti.row_count_estimate,
    ci.column_name, ci.ordinal_position, ci.data_type, ci.character_maximum_length,
    ci.numeric_precision, ci.numeric_scale, ci.is_nullable, ci.column_default, ci.column_comment
ORDER BY 
    ti.table_name, ci.ordinal_position;

-- =====================================================
-- RELATIONSHIP SUMMARY
-- =====================================================
\echo ''
\echo '=== FOREIGN KEY RELATIONSHIPS ==='
SELECT 
    tc.table_name as "From Table",
    kcu.column_name as "From Column", 
    ccu.table_name as "To Table",
    ccu.column_name as "To Column",
    tc.constraint_name as "Constraint Name"
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage ccu ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_schema = 'public'
ORDER BY tc.table_name;

-- =====================================================
-- VIEW DEFINITIONS
-- =====================================================
\echo ''
\echo '=== VIEWS ==='
SELECT 
    table_name as "View Name",
    view_definition as "Definition"
FROM information_schema.views 
WHERE table_schema = 'public'
ORDER BY table_name;

-- =====================================================
-- FUNCTIONS AND TRIGGERS
-- =====================================================
\echo ''
\echo '=== FUNCTIONS ==='
SELECT 
    routine_name as "Function Name",
    routine_type as "Type",
    data_type as "Return Type",
    routine_definition as "Definition"
FROM information_schema.routines 
WHERE routine_schema = 'public'
ORDER BY routine_name;

\echo ''
\echo '=== TRIGGERS ==='
SELECT 
    trigger_name as "Trigger Name",
    event_manipulation as "Event",
    event_object_table as "Table",
    action_statement as "Action"
FROM information_schema.triggers 
WHERE trigger_schema = 'public'
ORDER BY event_object_table, trigger_name;

-- =====================================================
-- SUMMARY STATISTICS
-- =====================================================
\echo ''
\echo '=== DATABASE SUMMARY ==='
SELECT 
    'Total Tables' as "Metric",
    COUNT(*)::text as "Value"
FROM information_schema.tables 
WHERE table_schema = 'public' AND table_type = 'BASE TABLE'

UNION ALL

SELECT 
    'Total Views' as "Metric",
    COUNT(*)::text as "Value"
FROM information_schema.views 
WHERE table_schema = 'public'

UNION ALL

SELECT 
    'Total Functions' as "Metric",
    COUNT(*)::text as "Value"
FROM information_schema.routines 
WHERE routine_schema = 'public'

UNION ALL

SELECT 
    'Database Size' as "Metric",
    pg_size_pretty(pg_database_size(current_database())) as "Value";