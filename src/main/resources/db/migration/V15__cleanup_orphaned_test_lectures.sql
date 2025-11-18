-- Cleanup orphaned test lectures with invalid file paths
-- These lectures have paths like "speakers/speaker-slug/collections/..." 
-- which were from testing before R2 storage was renewed with proper structure

-- First, let's see what we're deleting (for logging purposes)
-- Delete lectures that have file_path starting with "speakers/" or "catalog/"
-- but only if they don't match the actual R2 structure

-- Identify orphaned lectures (file_path doesn't match R2 structure)
-- R2 structure: "SpeakerName/CollectionName/XX - Title.mp3"
-- Old test structure: "speakers/speaker-slug/collections/collection-slug/lectures/file.mp3"

-- Delete lectures with old test path patterns
DELETE FROM lectures 
WHERE file_path LIKE 'speakers/%' 
   OR file_path LIKE 'catalog/%';

-- Note: This keeps all lectures that follow the R2 structure:
-- - Abdul Rashid Sufi/Quran Hafs/...
-- - Abdulrahman Hassan/Seerah of Prophet Muhammad ﷺ/...
-- - Ahmad Jibril/Legends Islam/...
-- - Bilal Assad/Those Who Desire Paradise/...
-- etc.
