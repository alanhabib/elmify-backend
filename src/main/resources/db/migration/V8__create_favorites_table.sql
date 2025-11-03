-- Create favorites table for user favorite lectures
CREATE TABLE IF NOT EXISTS favorites (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    lecture_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_favorites_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_favorites_lecture
        FOREIGN KEY (lecture_id)
        REFERENCES lectures(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_user_lecture
        UNIQUE (user_id, lecture_id)
);

-- Create indexes for efficient queries
CREATE INDEX idx_favorites_user_id ON favorites(user_id);
CREATE INDEX idx_favorites_lecture_id ON favorites(lecture_id);
CREATE INDEX idx_favorites_created_at ON favorites(created_at DESC);

-- Add comment
COMMENT ON TABLE favorites IS 'User favorite lectures - tracks which lectures users have saved';
COMMENT ON COLUMN favorites.user_id IS 'Reference to user (foreign key to users table)';
COMMENT ON COLUMN favorites.lecture_id IS 'Reference to the favorited lecture';
COMMENT ON COLUMN favorites.created_at IS 'When the lecture was added to favorites';
