-- V11__add_bio_and_description_fields.sql
-- Add bio field to speakers table and description field to collections table

BEGIN;

-- Add bio column to speakers table
ALTER TABLE speakers
ADD COLUMN bio TEXT;

COMMENT ON COLUMN speakers.bio IS 'Biography or description of the speaker';

-- Add description column to collections table
ALTER TABLE collections
ADD COLUMN description TEXT;

COMMENT ON COLUMN collections.description IS 'Description of what the collection covers';

COMMIT;
