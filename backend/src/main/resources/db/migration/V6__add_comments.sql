-- V6__add_comments.sql
-- Card comments for collaborative discussion on tasks

CREATE TABLE IF NOT EXISTS comments (
    id         BIGSERIAL PRIMARY KEY,
    card_id    BIGINT    NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    author_id  BIGINT    REFERENCES users(id) ON DELETE SET NULL,
    content    TEXT      NOT NULL,
    edited     BOOLEAN   NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Hot-path: fetch ordered thread for a single card
CREATE INDEX idx_comments_card_created ON comments(card_id, created_at ASC);

-- Lookup all comments authored by a user
CREATE INDEX idx_comments_author ON comments(author_id);

-- Auto-update updated_at on edit (reuses the function created in V1)
CREATE TRIGGER update_comments_updated_at
    BEFORE UPDATE ON comments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

