ALTER TABLE refresh_tokens
    ADD COLUMN IF NOT EXISTS last_used_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS device_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS user_agent VARCHAR(512),
    ADD COLUMN IF NOT EXISTS ip_address VARCHAR(64),
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_tenant
    ON refresh_tokens (user_id, tenant_id);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_active_sessions
    ON refresh_tokens (user_id, tenant_id, revoked_at, expires_at);
