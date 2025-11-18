-- Generated on 2025-11-18T08:41:56.615Z
-- Insert speakers from R2

BEGIN;

INSERT INTO speakers (id, name, created_at, updated_at, image_url, image_small_url, visibility_type, allowed_user_ids, is_premium)
VALUES
(1, 'Abdul Rashid Sufi', NOW(), NOW(), NULL, NULL, 'public', NULL, FALSE),
(2, 'Abdulrahman Hassan', NOW(), NOW(), NULL, NULL, 'public', NULL, FALSE),
(3, 'Ahmad Jibril', NOW(), NOW(), NULL, NULL, 'public', NULL, FALSE),
(4, 'Anwar Awlaki', NOW(), NOW(), NULL, NULL, 'public', NULL, FALSE),
(5, 'Badr al-Turki', NOW(), NOW(), NULL, NULL, 'public', NULL, FALSE),
(6, 'Bilal Assad', NOW(), NOW(), NULL, NULL, 'public', NULL, FALSE),
(7, 'Feiz Muhammad', NOW(), NOW(), NULL, NULL, 'public', NULL, FALSE),
(8, 'Maher al-Muaiqly', NOW(), NOW(), NULL, NULL, 'public', NULL, FALSE);

COMMIT;
