-- V11__add_missing_indexes.sql
-- Add missing indexes for authentication and common lookups

-- Authentication: username and email lookups happen on every request
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email    ON users(email);
