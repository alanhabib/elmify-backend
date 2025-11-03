-- V9: Add indexes for listening stats queries
-- Performance optimization for stats and streak calculations
-- Author: Backend Team
-- Date: 2025-10-08

-- ============================================================================
-- LISTENING STATS INDEXES
-- ============================================================================

-- Index for finding stats by user and date range (weekly progress, daily summary)
-- Used in: StatsService.getDailySummary(), StatsService.getWeeklyProgress()
CREATE INDEX IF NOT EXISTS idx_listening_stats_user_date
    ON listening_stats(user_id, date DESC);

COMMENT ON INDEX idx_listening_stats_user_date IS
    'Optimizes queries filtering by user_id and date range. Used for daily summaries and weekly progress.';

-- Index for finding stats by user, lecture, and date (upsert on track)
-- Used in: StatsService.trackListening() for finding existing stats
CREATE INDEX IF NOT EXISTS idx_listening_stats_user_lecture_date
    ON listening_stats(user_id, lecture_id, date);

COMMENT ON INDEX idx_listening_stats_user_lecture_date IS
    'Optimizes listening tracking upserts. Finds existing stats for a user/lecture/date combination.';

-- ============================================================================
-- USER INDEXES
-- ============================================================================

-- Index for faster user lookup by clerk_id (if not already exists from previous migrations)
-- Most queries use clerk_id from JWT authentication
CREATE INDEX IF NOT EXISTS idx_users_clerk_id
    ON users(clerk_id);

COMMENT ON INDEX idx_users_clerk_id IS
    'Optimizes user lookups by Clerk ID from JWT authentication tokens.';

-- ============================================================================
-- VERIFY INDEXES
-- ============================================================================

-- You can verify these indexes were created with:
-- SELECT indexname, tablename FROM pg_indexes WHERE tablename IN ('listening_stats', 'users');
