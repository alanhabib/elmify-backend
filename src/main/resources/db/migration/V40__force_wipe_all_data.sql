-- Force wipe all data (V32-V39 already ran, so this is V40)
-- This migration ONLY deletes data, does not reimport anything

BEGIN;

-- Delete dependent data first (respects foreign keys)
DELETE FROM user_saved_lectures;
DELETE FROM favorites;
DELETE FROM playback_positions;
DELETE FROM user_activities;
DELETE FROM listening_stats;
DELETE FROM lecture_play_credits;

-- Delete main entities
DELETE FROM lectures;
DELETE FROM collections;
DELETE FROM speakers;

-- Delete user data
DELETE FROM users;
DELETE FROM user_daily_goals;

COMMIT;
