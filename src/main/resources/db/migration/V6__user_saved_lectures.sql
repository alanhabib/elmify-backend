-- V6_user_saved_lectures.sql
-- Insert user saved lectures data (depends on lectures existing from V3)
BEGIN;

INSERT INTO user_saved_lectures (user_id, lecture_id, created_at)
VALUES ('user_314FCsjSjfucd6G6SGjoxAfHA8n', 79, 1755292560),
       ('user_314FCsjSjfucd6G6SGjoxAfHA8n', 108, 1755328232),
       ('user_31FLljmeOBf1RMptEaIIQHU9Pg2', 133, 1755436366),
       ('user_31gzjMNnNVzMFUKKRLXZdZypF9e', 133, 1755961434),
       ('user_30sZ4mdRbYN9ALg2z7jy9lVs2PM', 88, 1757389308),
       ('user_30sZ4mdRbYN9ALg2z7jy9lVs2PM', 231, 1757389979);

COMMIT;