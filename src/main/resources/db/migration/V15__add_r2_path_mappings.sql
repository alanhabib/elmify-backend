-- Add columns to store actual R2 folder names
-- This decouples database naming from R2 storage structure

ALTER TABLE speakers
ADD COLUMN r2_folder_name VARCHAR(255);

ALTER TABLE collections  
ADD COLUMN r2_folder_name VARCHAR(255);

-- Map speaker IDs to actual R2 folder names
UPDATE speakers SET r2_folder_name = 'Ahmad Jibril' WHERE id = 14;
UPDATE speakers SET r2_folder_name = 'Bilal Assad' WHERE id = 17;
UPDATE speakers SET r2_folder_name = 'Abdul Rashid Sufi' WHERE id = (SELECT id FROM speakers WHERE name = 'Abdul Rashid Sufi' LIMIT 1);

-- Map collection IDs to actual R2 folder names  
UPDATE collections SET r2_folder_name = 'Legends Islam' WHERE id = 34;
UPDATE collections SET r2_folder_name = 'Those Who Desire Paradise' WHERE id = 39;
UPDATE collections SET r2_folder_name = 'Quran Hafs' WHERE id = (SELECT id FROM collections WHERE title = 'Quran Hafs' LIMIT 1);

-- For collections without explicit mapping, use the title as-is
UPDATE collections 
SET r2_folder_name = title 
WHERE r2_folder_name IS NULL;

-- For speakers without explicit mapping, use the name as-is
UPDATE speakers
SET r2_folder_name = name
WHERE r2_folder_name IS NULL;
