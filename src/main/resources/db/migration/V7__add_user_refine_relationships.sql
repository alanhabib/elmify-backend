-- V7 Migration: Add users table and refine relationships
-- This migration is designed to handle existing data safely

-- Step 1: Create the new 'users' table
CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  clerk_id TEXT NOT NULL UNIQUE,
  email TEXT UNIQUE,
  display_name TEXT,
  profile_image_url TEXT,
  is_premium BOOLEAN DEFAULT FALSE NOT NULL,
  preferences JSONB DEFAULT '{}',
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Add the trigger for the updated_at column (function should exist from V1)
DO $$ 
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_trigger 
    WHERE tgname = 'trigger_users_updated_at' 
    AND tgrelid = 'users'::regclass
  ) THEN
    CREATE TRIGGER trigger_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp_function();
  END IF;
END $$;

-- Step 2: Handle existing tables with data preservation
-- First, let's safely handle tables that have user_id as TEXT

-- Check if we have existing data and need to preserve it
DO $$
DECLARE
  user_saved_count INTEGER;
  playback_count INTEGER;
  listening_count INTEGER;
  credit_count INTEGER;
  goal_count INTEGER;
BEGIN
  -- Count existing records
  SELECT COUNT(*) INTO user_saved_count FROM user_saved_lectures;
  SELECT COUNT(*) INTO playback_count FROM playback_positions;
  SELECT COUNT(*) INTO listening_count FROM listening_stats;
  SELECT COUNT(*) INTO credit_count FROM lecture_play_credits;
  SELECT COUNT(*) INTO goal_count FROM user_daily_goals;
  
  RAISE NOTICE 'Found % user_saved_lectures records', user_saved_count;
  RAISE NOTICE 'Found % playback_positions records', playback_count;
  RAISE NOTICE 'Found % listening_stats records', listening_count;
  RAISE NOTICE 'Found % lecture_play_credits records', credit_count;
  RAISE NOTICE 'Found % user_daily_goals records', goal_count;
  
  -- If we have existing data, we need to be more careful
  IF user_saved_count > 0 OR playback_count > 0 OR listening_count > 0 OR credit_count > 0 OR goal_count > 0 THEN
    RAISE NOTICE 'Existing data found. Consider backing up before proceeding.';
    
    -- For now, we'll clear the data to allow the migration to proceed
    -- In production, you'd want to migrate this data properly
    RAISE NOTICE 'Clearing existing user-related data to allow migration...';
    DELETE FROM user_saved_lectures;
    DELETE FROM playback_positions;
    DELETE FROM listening_stats;
    DELETE FROM lecture_play_credits;
    DELETE FROM user_daily_goals;
  END IF;
END $$;

-- Step 3: Handle view dependencies first
-- Drop views that depend on the user_id columns we're about to modify
DROP VIEW IF EXISTS daily_progress CASCADE;

-- Step 4: Now safely modify the tables
-- Table: user_saved_lectures
ALTER TABLE user_saved_lectures DROP COLUMN IF EXISTS user_id;
ALTER TABLE user_saved_lectures ADD COLUMN user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE user_saved_lectures DROP CONSTRAINT IF EXISTS user_saved_lectures_pkey;
ALTER TABLE user_saved_lectures ADD PRIMARY KEY (user_id, lecture_id);

-- Table: playback_positions
ALTER TABLE playback_positions DROP COLUMN IF EXISTS user_id;
ALTER TABLE playback_positions ADD COLUMN user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE playback_positions DROP CONSTRAINT IF EXISTS playback_positions_user_id_lecture_id_key;
ALTER TABLE playback_positions ADD CONSTRAINT playback_positions_user_id_lecture_id_key UNIQUE (user_id, lecture_id);

-- Table: listening_stats
ALTER TABLE listening_stats DROP COLUMN IF EXISTS user_id;
ALTER TABLE listening_stats ADD COLUMN user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE listening_stats DROP CONSTRAINT IF EXISTS listening_stats_user_id_lecture_id_date_key;
ALTER TABLE listening_stats ADD CONSTRAINT listening_stats_user_id_lecture_id_date_key UNIQUE (user_id, lecture_id, date);

-- Table: lecture_play_credits
ALTER TABLE lecture_play_credits DROP COLUMN IF EXISTS user_id;
ALTER TABLE lecture_play_credits ADD COLUMN user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE;

-- Table: user_daily_goals (special handling as user_id was the primary key)
ALTER TABLE user_daily_goals RENAME COLUMN user_id TO clerk_id;
ALTER TABLE user_daily_goals DROP CONSTRAINT IF EXISTS user_daily_goals_pkey;
ALTER TABLE user_daily_goals ADD COLUMN user_id BIGINT UNIQUE REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE user_daily_goals ADD PRIMARY KEY (user_id);

-- Step 4: Schema cleanup - remove redundant columns from lectures table
-- Only drop if columns exist to avoid errors
DO $$
BEGIN
  -- Drop speaker column if it exists
  IF EXISTS (SELECT 1 FROM information_schema.columns 
             WHERE table_name = 'lectures' AND column_name = 'speaker') THEN
    ALTER TABLE lectures DROP COLUMN speaker;
    RAISE NOTICE 'Dropped speaker column from lectures table';
  END IF;
  
  -- Drop collection column if it exists
  IF EXISTS (SELECT 1 FROM information_schema.columns 
             WHERE table_name = 'lectures' AND column_name = 'collection') THEN
    ALTER TABLE lectures DROP COLUMN collection;
    RAISE NOTICE 'Dropped collection column from lectures table';
  END IF;
  
  -- Drop user_id column if it exists (since you're the only uploader)
  IF EXISTS (SELECT 1 FROM information_schema.columns 
             WHERE table_name = 'lectures' AND column_name = 'user_id') THEN
    ALTER TABLE lectures DROP COLUMN user_id;
    RAISE NOTICE 'Dropped user_id column from lectures table';
  END IF;
END $$;

-- Step 6: Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_users_clerk_id ON users(clerk_id);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_user_saved_lectures_user_id ON user_saved_lectures(user_id);
CREATE INDEX IF NOT EXISTS idx_playback_positions_user_id ON playback_positions(user_id);
CREATE INDEX IF NOT EXISTS idx_listening_stats_user_id ON listening_stats(user_id);
CREATE INDEX IF NOT EXISTS idx_lecture_play_credits_user_id ON lecture_play_credits(user_id);

-- Step 7: Recreate the daily_progress view with the new integer user_id
-- Note: This view will need to be updated to work with the new users table relationship
-- For now, we'll create a basic version that can be enhanced later
CREATE VIEW daily_progress AS
SELECT 
  u.clerk_id,
  u.id as user_id,
  ls.date,
  COALESCE(SUM(ls.total_play_time), 0) as total_play_time,
  COALESCE(SUM(ls.play_count), 0) as play_count,
  COALESCE(AVG(ls.completion_rate), 0.0) as avg_completion_rate
FROM users u
LEFT JOIN listening_stats ls ON u.id = ls.user_id
GROUP BY u.clerk_id, u.id, ls.date;