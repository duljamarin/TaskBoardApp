-- V7__add_labels.sql
-- Labels (tags) for cards – scoped per board

CREATE TABLE IF NOT EXISTS labels (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(50)  NOT NULL,
    color      VARCHAR(20)  NOT NULL DEFAULT '#3498db',
    board_id   BIGINT       NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (board_id, name)
);

CREATE INDEX idx_labels_board ON labels(board_id);

-- Join table: many-to-many between cards and labels
CREATE TABLE IF NOT EXISTS card_labels (
    card_id  BIGINT NOT NULL REFERENCES cards(id)  ON DELETE CASCADE,
    label_id BIGINT NOT NULL REFERENCES labels(id) ON DELETE CASCADE,
    PRIMARY KEY (card_id, label_id)
);

CREATE INDEX idx_card_labels_card  ON card_labels(card_id);
CREATE INDEX idx_card_labels_label ON card_labels(label_id);

-- Auto-update updated_at on labels
CREATE TRIGGER update_labels_updated_at
    BEFORE UPDATE ON labels
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Seed some default labels for existing boards
INSERT INTO labels (name, color, board_id)
SELECT 'Bug',         '#e74c3c', b.id FROM boards b WHERE NOT b.archived
UNION ALL
SELECT 'Feature',     '#2ecc71', b.id FROM boards b WHERE NOT b.archived
UNION ALL
SELECT 'Enhancement', '#3498db', b.id FROM boards b WHERE NOT b.archived
UNION ALL
SELECT 'Urgent',      '#e67e22', b.id FROM boards b WHERE NOT b.archived
UNION ALL
SELECT 'Documentation','#9b59b6', b.id FROM boards b WHERE NOT b.archived;

