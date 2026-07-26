-- Convert timestamp columns to timestamptz to store absolute UTC instants
-- Prevents timezone-offset bugs when writing java.time.Instant values via JDBC

ALTER TABLE users
ALTER COLUMN created_at TYPE TIMESTAMPTZ;

ALTER TABLE sessions
ALTER COLUMN expires_at TYPE TIMESTAMPTZ,
    ALTER COLUMN created_at TYPE TIMESTAMPTZ;

-- Clear out old sessions stored with the incorrect (offset) timestamp values
DELETE FROM sessions;