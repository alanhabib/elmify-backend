-- V1_02_Core_Data.sql
BEGIN;

INSERT INTO speakers (id, name, created_at, updated_at, image_url, image_small_url, visibility_type, allowed_user_ids,
                      is_premium)
VALUES (14, 'Ahmad Jibril', '2025-08-14 20:08:38', '2025-08-17 05:43:00',
        'speakers/14/1755409378503-i8nepm-speaker-image.jpg', NULL, 'public', NULL, FALSE),
       (16, 'Anwar al-Awlaki', '2025-08-14 20:10:35', '2025-08-21 16:03:16',
        'speakers/16/1755409400542-b0z48s-speaker-image.jpg', NULL, 'premium', NULL, FALSE),
       (17, 'Bilal Assad', '2025-08-14 20:15:51', '2025-08-17 05:44:44',
        'speakers/17/1755409482602-j0713f-speaker-image.jpeg', NULL, 'public', NULL, FALSE),
       (18, 'Abu Adnan', '2025-08-19 13:33:10', '2025-08-19 13:40:15',
        'speakers/18/1755610812582-xndzs8-speaker-image.jpg', NULL, 'public', NULL, FALSE),
       (19, 'Feiz Muhammad', '2025-08-19 13:33:51', '2025-08-19 13:40:24',
        'speakers/19/1755610822900-p75sre-speaker-image.jpg', NULL, 'public', NULL, FALSE),
       (20, 'Kamal el-Mekki', '2025-08-19 13:34:24', '2025-08-19 13:40:33',
        'speakers/20/1755610831402-6xpor3-speaker-image.png', NULL, 'public', NULL, FALSE);

INSERT INTO collections (id, speaker_id, title, year, cover_image_url, created_at, updated_at, cover_image_small_url)
VALUES (34, 14, 'Legends of Islam', 2025, 'collections/34/1755409381435-79ta78-cover.jpg', '2025-08-14 20:08:41',
        '2025-08-17 05:43:03', NULL),
       (38, 16, 'Lives of the prophets', 2025, 'collections/38/1755409403619-phfz2k-cover.jpg', '2025-08-14 20:10:38',
        '2025-08-17 05:43:25', NULL),
       (39, 17, 'Those who desire Paradise', 2025, 'collections/39/1755409485480-wpq1kw-cover.jpg',
        '2025-08-14 20:15:55', '2025-08-17 05:44:47', NULL),
       (40, 16, 'Life of Prophet Muhammad (Mecka)', 2025, 'collections/40/1755409417939-hn1ys5-cover.jpg',
        '2025-08-16 20:59:32', '2025-08-17 05:43:40', NULL),
       (41, 16, 'Life of Prophet Muhammad (Medina 1)', 2025, 'collections/41/1755409429528-gwqjqv-cover.jpg',
        '2025-08-16 21:02:55', '2025-08-17 05:43:51', NULL),
       (42, 16, 'Life of Prophet Muhammad (Medina 2)', 2025, 'collections/42/1755409442300-9iu1dw-cover.jpg',
        '2025-08-16 21:07:07', '2025-08-17 05:44:04', NULL),
       (43, 16, 'The Hereafter', 2025, 'collections/43/1755409455909-b64i37-cover.jpg', '2025-08-16 21:13:22',
        '2025-08-17 05:44:18', NULL),
       (44, 18, 'Al Mahdi', 2025, 'collections/44/1755610815829-can1xq-cover.jpg', '2025-08-19 13:33:14',
        '2025-08-19 13:40:17', NULL),
       (45, 18, 'Treating Waswaas', 2025, 'collections/45/1755610819277-bxg7xb-cover.jpg', '2025-08-19 13:33:28',
        '2025-08-19 13:40:21', NULL),
       (46, 19, 'The etiquettes of Hijab', 2025, 'collections/46/1755610825800-6rtz6o-cover.jpg', '2025-08-19 13:33:55',
        '2025-08-19 13:40:28', NULL),
       (47, 20, 'Haya & Iman', 2025, 'collections/47/1755610834113-wg0mpe-cover.jpg', '2025-08-19 13:34:29',
        '2025-08-19 13:40:36', NULL),
       (48, 20, 'Strengthening Ones Memory', 2025, 'collections/48/1755610838474-blqm4i-cover.jpg',
        '2025-08-19 13:34:50', '2025-08-19 13:40:40', NULL);


COMMIT;
