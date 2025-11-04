-- V10 Migration: Create user_activities table for tracking user actions
CREATE TABLE user_activities (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    activity_type VARCHAR(50) NOT NULL,
    description TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    session_id VARCHAR(255),
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for efficient queries
CREATE INDEX idx_user_activities_user_id ON user_activities(user_id);
CREATE INDEX idx_user_activities_activity_type ON user_activities(activity_type);
CREATE INDEX idx_user_activities_created_at ON user_activities(created_at DESC);
CREATE INDEX idx_user_activities_user_activity_time ON user_activities(user_id, activity_type, created_at DESC);

-- Add comment
COMMENT ON TABLE user_activities IS 'Tracks all user activities and actions in the system';
COMMENT ON COLUMN user_activities.user_id IS 'Reference to the user who performed the activity';
COMMENT ON COLUMN user_activities.activity_type IS 'Type of activity performed (LOGIN, LECTURE_PLAY, etc.)';
COMMENT ON COLUMN user_activities.metadata IS 'JSON metadata for additional activity information';
