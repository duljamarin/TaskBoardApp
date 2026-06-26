-- Board members table for collaborative board access
CREATE TABLE board_members (
    id          BIGSERIAL PRIMARY KEY,
    board_id    BIGINT      NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_board_members_board_user UNIQUE (board_id, user_id)
);

CREATE INDEX idx_board_members_user ON board_members(user_id);
CREATE INDEX idx_board_members_board ON board_members(board_id);

-- Seed: every existing board owner becomes an OWNER member
INSERT INTO board_members (board_id, user_id, role)
SELECT id, owner_id, 'OWNER'
FROM boards
WHERE owner_id IS NOT NULL
ON CONFLICT DO NOTHING;
