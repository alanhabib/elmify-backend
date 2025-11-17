-- Add audio_url column to lectures table to store full R2 public URLs
-- This allows iOS AVPlayer to stream directly without presigned URL generation
ALTER TABLE lectures
ADD COLUMN audio_url VARCHAR(1024);

-- Populate audio_url with full R2 public URLs based on file_path
-- Format: https://pub-YOUR-R2-ID.r2.dev/file_path
UPDATE lectures
SET audio_url = 'https://pub-f62fc66a08a14cbab5f1fc2f27d3b71d.r2.dev/' || file_path
WHERE file_path IS NOT NULL AND audio_url IS NULL;

-- Create index for faster lookups
CREATE INDEX idx_lectures_audio_url ON lectures(audio_url);
