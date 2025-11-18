-- Generated on 2025-11-18T11:19:03.159Z
-- Clean all existing data before fresh R2 import

BEGIN;

-- Delete dependent data first
DELETE FROM user_saved_lectures;
DELETE FROM favorites;
DELETE FROM playback_positions;
DELETE FROM user_activities;

-- Delete main entities
DELETE FROM lectures;
DELETE FROM collections;
DELETE FROM speakers;

COMMIT;
