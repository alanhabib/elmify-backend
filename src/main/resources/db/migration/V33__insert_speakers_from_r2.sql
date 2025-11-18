-- Generated on 2025-11-18T11:19:03.159Z
-- Insert speakers from R2

BEGIN;

INSERT INTO speakers (id, name, created_at, updated_at, image_url, image_small_url, visibility_type, allowed_user_ids, is_premium)
VALUES
(1, 'Abdul Rashid Sufi', NOW(), NOW(), 'https://cdn.elmify.store/Abdul Rashid Sufi/speaker.jpg', 'https://cdn.elmify.store/Abdul Rashid Sufi/speaker_small.jpg', 'public', NULL, FALSE),
(2, 'Abdulrahman Hassan', NOW(), NOW(), 'https://cdn.elmify.store/Abdulrahman Hassan/speaker.jpg', 'https://cdn.elmify.store/Abdulrahman Hassan/speaker_small.jpg', 'public', NULL, FALSE),
(3, 'Ahmad Jibril', NOW(), NOW(), 'https://cdn.elmify.store/Ahmad Jibril/speaker.jpg', 'https://cdn.elmify.store/Ahmad Jibril/speaker_small.jpg', 'public', NULL, FALSE),
(4, 'Anwar Awlaki', NOW(), NOW(), 'https://cdn.elmify.store/Anwar Awlaki/speaker.jpg', 'https://cdn.elmify.store/Anwar Awlaki/speaker_small.jpg', 'public', NULL, FALSE),
(5, 'Badr al-Turki', NOW(), NOW(), NULL, NULL, 'public', NULL, FALSE),
(6, 'Bilal Assad', NOW(), NOW(), 'https://cdn.elmify.store/Bilal Assad/speaker.jpg', 'https://cdn.elmify.store/Bilal Assad/speaker_small.jpg', 'public', NULL, FALSE),
(7, 'Feiz Muhammad', NOW(), NOW(), 'https://cdn.elmify.store/Feiz Muhammad/speaker.jpg', 'https://cdn.elmify.store/Feiz Muhammad/speaker_small.jpg', 'public', NULL, FALSE),
(8, 'Maher al-Muaiqly', NOW(), NOW(), 'https://cdn.elmify.store/Maher al-Muaiqly/speaker.png', 'https://cdn.elmify.store/Maher al-Muaiqly/speaker_small.png', 'public', NULL, FALSE);

COMMIT;
