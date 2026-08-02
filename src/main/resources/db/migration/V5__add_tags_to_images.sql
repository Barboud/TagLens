ALTER TABLE images ADD COLUMN tags JSONB;

CREATE INDEX idx_images_tags ON images USING GIN (tags);