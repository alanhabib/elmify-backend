-- V1_01_Schema.sql
BEGIN;

-- Set PostgreSQL-specific settings
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

-- Table Definitions
CREATE TABLE speakers (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL UNIQUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  image_url TEXT,
  image_small_url TEXT,
  visibility_type TEXT DEFAULT 'public' CHECK (visibility_type IN ('public', 'premium', 'restricted')),
  allowed_user_ids TEXT,
  is_premium BOOLEAN DEFAULT FALSE
);

CREATE TABLE collections (
  id BIGSERIAL PRIMARY KEY,
  speaker_id BIGINT NOT NULL REFERENCES speakers(id) ON DELETE CASCADE,
  title TEXT NOT NULL,
  year INTEGER,
  cover_image_url TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  cover_image_small_url TEXT,
  UNIQUE(speaker_id, title)
);

CREATE TABLE lectures (
  id BIGSERIAL PRIMARY KEY,
  user_id INTEGER,
  directory_id INTEGER,
  title TEXT NOT NULL,
  speaker TEXT,
  collection TEXT,
  genre TEXT,
  year INTEGER,
  duration INTEGER NOT NULL,
  file_name TEXT NOT NULL,
  file_path TEXT NOT NULL,
  file_size BIGINT NOT NULL,
  file_format TEXT NOT NULL,
  bitrate INTEGER,
  sample_rate INTEGER,
  file_hash TEXT,
  thumbnail_url TEXT,
  waveform_data TEXT,
  is_public BOOLEAN DEFAULT FALSE,
  play_count INTEGER DEFAULT 0,
  uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_played_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  speaker_id BIGINT REFERENCES speakers(id) ON DELETE CASCADE,
  collection_id BIGINT REFERENCES collections(id) ON DELETE CASCADE,
  description TEXT,
  lecture_number integer
);

CREATE TABLE user_saved_lectures (
  user_id TEXT NOT NULL,
  lecture_id BIGINT NOT NULL REFERENCES lectures(id) ON DELETE CASCADE,
  created_at BIGINT NOT NULL DEFAULT (extract(epoch from now())),
  PRIMARY KEY (user_id, lecture_id)
);

CREATE TABLE playback_positions (
  id BIGSERIAL PRIMARY KEY,
  user_id TEXT NOT NULL,
  lecture_id BIGINT NOT NULL REFERENCES lectures(id) ON DELETE CASCADE,
  current_position INTEGER NOT NULL DEFAULT 0,
  last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(user_id, lecture_id)
);

CREATE TABLE listening_stats (
  id BIGSERIAL PRIMARY KEY,
  user_id TEXT NOT NULL,
  lecture_id BIGINT NOT NULL REFERENCES lectures(id) ON DELETE CASCADE,
  date DATE NOT NULL,
  total_play_time INTEGER DEFAULT 0,
  play_count INTEGER DEFAULT 0,
  completion_rate REAL DEFAULT 0.0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(user_id, lecture_id, date)
);

CREATE TABLE user_daily_goals (
  user_id TEXT PRIMARY KEY,
  daily_goal_minutes INTEGER DEFAULT 15 NOT NULL,
  current_streak INTEGER DEFAULT 0 NOT NULL,
  best_streak INTEGER DEFAULT 0 NOT NULL,
  current_streak_start_date DATE,
  last_goal_met_date DATE,
  timezone TEXT DEFAULT 'UTC' NOT NULL,
  is_active BOOLEAN DEFAULT TRUE NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  longest_streak INTEGER DEFAULT 0,
  longest_streak_start_date DATE,
  longest_streak_end_date DATE
);

CREATE TABLE lecture_play_credits (
  id BIGSERIAL PRIMARY KEY,
  user_id TEXT NOT NULL,
  lecture_id BIGINT NOT NULL REFERENCES lectures(id) ON DELETE CASCADE,
  day INTEGER NOT NULL,
  played_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP
);

-- Trigger Function
CREATE OR REPLACE FUNCTION update_timestamp_function()
RETURNS TRIGGER AS $$
BEGIN
   NEW.updated_at = NOW();
   RETURN NEW;
END;
$$ language 'plpgsql';

-- View Definitions
CREATE VIEW daily_progress AS
SELECT
  user_id,
  date,
  SUM(total_play_time) as daily_seconds,
  ROUND(SUM(total_play_time) / 60.0) as daily_minutes,
  COUNT(DISTINCT lecture_id) as lectures_count,
  MAX(created_at) as last_activity
FROM listening_stats
GROUP BY user_id, date
ORDER BY user_id, date DESC;

COMMIT;
