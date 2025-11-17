-- Populate audio_url with CDN domain for direct iOS streaming
-- Using custom Cloudflare R2 domain for better performance
UPDATE lectures
SET audio_url = 'https://cdn.elmify.store/' || file_path
WHERE file_path IS NOT NULL;
