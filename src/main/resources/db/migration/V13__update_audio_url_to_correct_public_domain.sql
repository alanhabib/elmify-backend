-- Update audio_url to use correct public R2 domain
-- Previous migration V12 used wrong subdomain
UPDATE lectures
SET audio_url = REPLACE(audio_url, 'https://pub-f62fc66a08a14cbab5f1fc2f27d3b71d.r2.dev/', 'https://pub-c3e9209786b34415821b131478044dd5.r2.dev/')
WHERE audio_url LIKE 'https://pub-f62fc66a08a14cbab5f1fc2f27d3b71d.r2.dev/%';
