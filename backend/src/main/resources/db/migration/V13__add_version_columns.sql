-- Add optimistic locking version columns to prevent lost updates on concurrent modifications.
ALTER TABLE boards ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE board_lists ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE cards ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE comments ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
