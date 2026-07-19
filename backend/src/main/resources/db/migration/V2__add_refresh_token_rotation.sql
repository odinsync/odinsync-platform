ALTER TABLE refresh_tokens
    ALTER COLUMN token_hash TYPE VARCHAR(64),
    ADD COLUMN family_id UUID,
    ADD COLUMN issued_at TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP,
    ADD COLUMN replaced_by_token_id UUID;

UPDATE refresh_tokens
SET family_id = id,
    issued_at = created_at,
    updated_at = created_at
WHERE family_id IS NULL;

ALTER TABLE refresh_tokens
    ALTER COLUMN family_id SET NOT NULL,
    ALTER COLUMN issued_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_replacement
        FOREIGN KEY (replaced_by_token_id) REFERENCES refresh_tokens (id);

CREATE INDEX idx_refresh_tokens_family_id
    ON refresh_tokens (family_id);
