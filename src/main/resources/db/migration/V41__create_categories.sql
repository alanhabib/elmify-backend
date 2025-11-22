-- V41__create_categories.sql
-- Create category system tables with proper normalization

-- Categories table
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    icon_name VARCHAR(50) DEFAULT 'folder-outline',
    color VARCHAR(7) DEFAULT '#a855f7',
    parent_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    display_order INTEGER DEFAULT 0,
    is_featured BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    lecture_count INTEGER DEFAULT 0,
    collection_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Junction table for lecture-category (many-to-many)
CREATE TABLE lecture_categories (
    lecture_id BIGINT NOT NULL REFERENCES lectures(id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    is_primary BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (lecture_id, category_id)
);

-- Junction table for collection-category (many-to-many)
CREATE TABLE collection_categories (
    collection_id BIGINT NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    is_primary BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (collection_id, category_id)
);

-- Indexes for performance
CREATE INDEX idx_categories_parent ON categories(parent_id);
CREATE INDEX idx_categories_slug ON categories(slug);
CREATE INDEX idx_categories_featured ON categories(is_featured) WHERE is_featured = true;
CREATE INDEX idx_categories_active ON categories(is_active) WHERE is_active = true;
CREATE INDEX idx_categories_display_order ON categories(display_order);
CREATE INDEX idx_lecture_categories_category ON lecture_categories(category_id);
CREATE INDEX idx_lecture_categories_primary ON lecture_categories(is_primary) WHERE is_primary = true;
CREATE INDEX idx_collection_categories_category ON collection_categories(category_id);

-- Trigger function to update lecture_count
CREATE OR REPLACE FUNCTION update_category_lecture_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE categories SET lecture_count = lecture_count + 1, updated_at = CURRENT_TIMESTAMP WHERE id = NEW.category_id;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE categories SET lecture_count = lecture_count - 1, updated_at = CURRENT_TIMESTAMP WHERE id = OLD.category_id;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_lecture_categories_count
AFTER INSERT OR DELETE ON lecture_categories
FOR EACH ROW EXECUTE FUNCTION update_category_lecture_count();

-- Trigger function to update collection_count
CREATE OR REPLACE FUNCTION update_category_collection_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE categories SET collection_count = collection_count + 1, updated_at = CURRENT_TIMESTAMP WHERE id = NEW.category_id;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE categories SET collection_count = collection_count - 1, updated_at = CURRENT_TIMESTAMP WHERE id = OLD.category_id;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_collection_categories_count
AFTER INSERT OR DELETE ON collection_categories
FOR EACH ROW EXECUTE FUNCTION update_category_collection_count();
