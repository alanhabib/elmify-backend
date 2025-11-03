-- V1_05_Finalize.sql
BEGIN;

-- Create Indexes
CREATE INDEX idx_collections_speaker_id ON collections(speaker_id);
CREATE INDEX idx_collections_title ON collections(title);
CREATE INDEX idx_speakers_name ON speakers(name);
CREATE INDEX idx_lectures_title ON lectures(title);
CREATE INDEX idx_lectures_speaker ON lectures(speaker);
CREATE INDEX idx_lectures_genre ON lectures(genre);
CREATE INDEX idx_lectures_public ON lectures(is_public);
CREATE INDEX idx_lectures_uploaded_at ON lectures(uploaded_at);
CREATE INDEX idx_lectures_play_count ON lectures(play_count);
CREATE INDEX idx_lectures_speaker_collection_ids ON lectures(speaker_id, collection_id);
CREATE INDEX idx_playback_positions_user_lecture ON playback_positions(user_id, lecture_id);
CREATE INDEX idx_playback_positions_last_updated ON playback_positions(last_updated);
CREATE INDEX idx_listening_stats_user_date ON listening_stats(user_id, date);
CREATE INDEX idx_listening_stats_date ON listening_stats(date);
CREATE INDEX idx_listening_stats_lecture ON listening_stats(lecture_id);
CREATE INDEX idx_user_goals_streak_dates ON user_daily_goals(current_streak_start_date, last_goal_met_date);
CREATE INDEX idx_user_goals_active ON user_daily_goals(is_active) WHERE is_active = true;
CREATE UNIQUE INDEX idx_lecture_play_credits_unique ON lecture_play_credits(user_id, lecture_id, day);
CREATE INDEX idx_lecture_play_credits_user_played_at ON lecture_play_credits(user_id, played_at DESC);
CREATE INDEX idx_lecture_play_credits_lecture_id ON lecture_play_credits(lecture_id);
CREATE INDEX idx_lecture_play_credits_user_id ON lecture_play_credits(user_id);
CREATE INDEX idx_lecture_play_credits_created_at ON lecture_play_credits(created_at);
CREATE INDEX idx_user_daily_goals_user_id ON user_daily_goals(user_id);

-- Apply Triggers
CREATE TRIGGER update_speakers_timestamp
    BEFORE UPDATE ON speakers
    FOR EACH ROW EXECUTE FUNCTION update_timestamp_function();

CREATE TRIGGER update_collections_timestamp
    BEFORE UPDATE ON collections
    FOR EACH ROW EXECUTE FUNCTION update_timestamp_function();

-- Reset sequences to prevent ID conflicts
SELECT setval('speakers_id_seq', (SELECT MAX(id) FROM speakers));
SELECT setval('collections_id_seq', (SELECT MAX(id) FROM collections));
SELECT setval('lectures_id_seq', (SELECT MAX(id) FROM lectures));
SELECT setval('playback_positions_id_seq', (SELECT MAX(id) FROM playback_positions));
SELECT setval('listening_stats_id_seq', (SELECT MAX(id) FROM listening_stats));
SELECT setval('lecture_play_credits_id_seq', (SELECT MAX(id) FROM lecture_play_credits));

COMMIT;
