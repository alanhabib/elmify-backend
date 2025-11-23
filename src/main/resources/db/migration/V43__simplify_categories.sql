-- V43__simplify_categories.sql
-- Replace complex category hierarchy with simplified flat structure

-- Clear existing category assignments
DELETE FROM lecture_categories;
DELETE FROM collection_categories;

-- Clear existing categories
DELETE FROM categories;

-- Insert simplified categories (no parent_id, flat structure)
INSERT INTO categories (name, slug, description, icon_name, color, display_order, is_featured, is_active) VALUES
('Quran', 'quran', 'Recitations, tajweed, and memorization', 'book-outline', '#10B981', 1, true, true),
('Tafsir', 'tafsir', 'Quran interpretation and commentary', 'document-text-outline', '#3B82F6', 2, true, true),
('Aqeedah', 'aqeedah', 'Islamic creed, beliefs, and tawheed', 'shield-checkmark-outline', '#8B5CF6', 3, true, true),
('Seerah', 'seerah', 'Biography of Prophet Muhammad ﷺ', 'person-outline', '#F59E0B', 4, true, true),
('Sahaba', 'sahaba', 'Stories of the Companions', 'people-outline', '#EC4899', 5, true, true),
('Fiqh', 'fiqh', 'Islamic jurisprudence and rulings', 'scale-outline', '#6366F1', 6, true, true),
('Spirituality', 'spirituality', 'Heart purification, dhikr, and tawbah', 'heart-outline', '#14B8A6', 7, true, true),
('Family', 'family', 'Marriage, parenting, and youth', 'home-outline', '#F97316', 8, true, true),
('History', 'history', 'Islamic civilization and scholars', 'library-outline', '#64748B', 9, true, true);
