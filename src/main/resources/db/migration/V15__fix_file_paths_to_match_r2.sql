-- Fix file paths to match actual R2 bucket structure
-- R2 structure: Speaker Name/Collection Name/XX - Lecture Title.mp3
-- Database had: speakers/speaker-slug/collections/collection-slug/lectures/xx-lecture-slug.mp3

-- Example fixes based on actual R2 structure:
-- Bilal Assad / Those Who Desire Paradise
UPDATE lectures 
SET file_path = 'Bilal Assad/Those Who Desire Paradise/' || 
    LPAD(lecture_number::text, 2, '0') || ' - ' || 
    LPAD(lecture_number::text, 2, '0') || ' ' || title || '.mp3'
WHERE speaker_id = 17 AND collection_id = 39 AND file_path IS NOT NULL;

-- Update audio_url to match new file_path
UPDATE lectures
SET audio_url = 'https://cdn.elmify.store/' || file_path
WHERE file_path IS NOT NULL AND audio_url IS NOT NULL;
