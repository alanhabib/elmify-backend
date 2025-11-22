-- V42__seed_categories.sql
-- Seed initial category hierarchy

-- Top-level categories
INSERT INTO categories (name, slug, description, icon_name, color, display_order, is_featured) VALUES
('Quran & Tafsir', 'quran-tafsir', 'Quran recitation, interpretation, and tajweed', 'book-outline', '#10B981', 1, true),
('Islamic History', 'islamic-history', 'Seerah, companions, and Islamic civilization', 'library-outline', '#F59E0B', 2, true),
('Fiqh & Jurisprudence', 'fiqh', 'Islamic law, worship, and transactions', 'scale-outline', '#6366F1', 3, true),
('Spirituality & Heart', 'spirituality', 'Soul purification, character, and remembrance', 'heart-outline', '#EC4899', 4, true),
('Family & Relationships', 'family', 'Marriage, parenting, and family life', 'people-outline', '#8B5CF6', 5, true),
('Knowledge & Learning', 'knowledge', 'Seeking knowledge and Islamic sciences', 'school-outline', '#3B82F6', 6, false),
('Contemporary Issues', 'contemporary', 'Modern challenges and social issues', 'globe-outline', '#14B8A6', 7, false),
('Personal Development', 'personal-development', 'Productivity, mindset, and growth', 'trending-up-outline', '#F97316', 8, false);

-- Subcategories for Quran & Tafsir
INSERT INTO categories (name, slug, description, icon_name, color, parent_id, display_order) VALUES
('Quran Recitation', 'quran-recitation', 'Beautiful recitations and memorization', 'musical-notes-outline', '#10B981',
    (SELECT id FROM categories WHERE slug = 'quran-tafsir'), 1),
('Tafsir Studies', 'tafsir-studies', 'In-depth Quran interpretation', 'document-text-outline', '#10B981',
    (SELECT id FROM categories WHERE slug = 'quran-tafsir'), 2),
('Tajweed', 'tajweed', 'Rules of Quran recitation', 'mic-outline', '#10B981',
    (SELECT id FROM categories WHERE slug = 'quran-tafsir'), 3);

-- Subcategories for Islamic History
INSERT INTO categories (name, slug, description, icon_name, color, parent_id, display_order) VALUES
('Prophetic Biography', 'seerah', 'Life of Prophet Muhammad (PBUH)', 'person-outline', '#F59E0B',
    (SELECT id FROM categories WHERE slug = 'islamic-history'), 1),
('Companions', 'sahaba', 'Stories of the Sahaba', 'people-circle-outline', '#F59E0B',
    (SELECT id FROM categories WHERE slug = 'islamic-history'), 2),
('Islamic Civilization', 'islamic-civilization', 'History of Islamic empires and scholars', 'business-outline', '#F59E0B',
    (SELECT id FROM categories WHERE slug = 'islamic-history'), 3);

-- Subcategories for Fiqh
INSERT INTO categories (name, slug, description, icon_name, color, parent_id, display_order) VALUES
('Worship', 'ibadah', 'Prayer, fasting, zakah, and hajj', 'hand-left-outline', '#6366F1',
    (SELECT id FROM categories WHERE slug = 'fiqh'), 1),
('Transactions', 'muamalat', 'Business, finance, and contracts', 'cash-outline', '#6366F1',
    (SELECT id FROM categories WHERE slug = 'fiqh'), 2),
('Family Law', 'family-law', 'Marriage, divorce, and inheritance', 'home-outline', '#6366F1',
    (SELECT id FROM categories WHERE slug = 'fiqh'), 3);

-- Subcategories for Family & Relationships
INSERT INTO categories (name, slug, description, icon_name, color, parent_id, display_order) VALUES
('Marriage', 'marriage', 'Building strong Islamic marriages', 'heart-circle-outline', '#8B5CF6',
    (SELECT id FROM categories WHERE slug = 'family'), 1),
('Parenting', 'parenting', 'Raising righteous children', 'happy-outline', '#8B5CF6',
    (SELECT id FROM categories WHERE slug = 'family'), 2),
('Youth Guidance', 'youth', 'Guidance for young Muslims', 'school-outline', '#8B5CF6',
    (SELECT id FROM categories WHERE slug = 'family'), 3);

-- Subcategories for Spirituality
INSERT INTO categories (name, slug, description, icon_name, color, parent_id, display_order) VALUES
('Purification of Soul', 'tazkiyah', 'Self-improvement and spiritual growth', 'sparkles-outline', '#EC4899',
    (SELECT id FROM categories WHERE slug = 'spirituality'), 1),
('Character Development', 'akhlaq', 'Islamic manners and ethics', 'ribbon-outline', '#EC4899',
    (SELECT id FROM categories WHERE slug = 'spirituality'), 2),
('Remembrance', 'dhikr', 'Duas, adhkar, and connection with Allah', 'moon-outline', '#EC4899',
    (SELECT id FROM categories WHERE slug = 'spirituality'), 3);
