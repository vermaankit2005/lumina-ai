-- ============================================================
-- LUMINA AI – PostgreSQL Schema v1.0 (MVP)
-- Compatible: PostgreSQL 15+
-- ============================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- TABLE: oauth_credentials
-- Stores encrypted OAuth2 tokens for Gmail
-- Single-row table (personal tool, single user)
-- ============================================================
CREATE TABLE oauth_credentials (
    id                    BIGSERIAL PRIMARY KEY,
    user_email            VARCHAR(255)  NOT NULL,
    access_token_enc      TEXT          NOT NULL,  -- AES-256 encrypted
    refresh_token_enc     TEXT          NOT NULL,  -- AES-256 encrypted
    token_expiry_utc      TIMESTAMP     NOT NULL,
    created_at            TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_oauth_user_email UNIQUE (user_email)
);

COMMENT ON TABLE oauth_credentials IS 'Stores encrypted OAuth2 tokens. Single row per provider.';
COMMENT ON COLUMN oauth_credentials.access_token_enc IS 'AES-256-GCM encrypted access token';
COMMENT ON COLUMN oauth_credentials.refresh_token_enc IS 'AES-256-GCM encrypted refresh token';

-- ============================================================
-- TABLE: briefing_runs
-- Audit log of every daily briefing execution
-- ============================================================
CREATE TABLE briefing_runs (
    id                    BIGSERIAL PRIMARY KEY,
    run_date              DATE          NOT NULL,
    status                VARCHAR(30)   NOT NULL DEFAULT 'PENDING',    -- PENDING | RUNNING | SUCCESS | FAILED
    emails_fetched        INTEGER       DEFAULT 0,
    tasks_extracted       INTEGER       DEFAULT 0,
    error_message         TEXT,
    briefing_markdown     TEXT,         -- full generated briefing stored for replay
    started_at            TIMESTAMP     NOT NULL DEFAULT NOW(),
    completed_at          TIMESTAMP,
    CONSTRAINT uq_briefing_run_date UNIQUE (run_date, started_at)
);

CREATE INDEX idx_briefing_runs_date ON briefing_runs (run_date DESC);
CREATE INDEX idx_briefing_runs_status ON briefing_runs (status);

-- ============================================================
-- TABLE: action_tasks
-- Persistent memory of AI-extracted action items from emails
-- ============================================================
CREATE TABLE action_tasks (
    id                    BIGSERIAL PRIMARY KEY,
    external_id           UUID          NOT NULL DEFAULT gen_random_uuid(),  -- exposed in Telegram commands
    briefing_run_id       BIGINT        REFERENCES briefing_runs(id) ON DELETE SET NULL,
    title                 VARCHAR(500)  NOT NULL,
    description           TEXT,
    priority              VARCHAR(10)   NOT NULL DEFAULT 'MEDIUM',  -- HIGH | MEDIUM | LOW
    status                VARCHAR(20)   NOT NULL DEFAULT 'OPEN',    -- OPEN | DONE | DISMISSED
    source_email_id       VARCHAR(255),    -- Gmail message ID
    source_sender         VARCHAR(255),    -- From: header
    source_subject        VARCHAR(500),    -- Email subject
    deadline_date         DATE,            -- NULL if no deadline inferred
    completed_at          TIMESTAMP,
    created_at            TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_priority CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT chk_status   CHECK (status IN ('OPEN', 'DONE', 'DISMISSED'))
);

CREATE INDEX idx_action_tasks_status ON action_tasks (status);
CREATE INDEX idx_action_tasks_priority_status ON action_tasks (priority, status);
CREATE INDEX idx_action_tasks_deadline ON action_tasks (deadline_date) WHERE deadline_date IS NOT NULL;
CREATE INDEX idx_action_tasks_source_email ON action_tasks (source_email_id);
CREATE INDEX idx_action_tasks_external_id ON action_tasks (external_id);
CREATE INDEX idx_action_tasks_briefing_run ON action_tasks (briefing_run_id);

-- ============================================================
-- TRIGGER: auto-update updated_at timestamps
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trg_oauth_credentials_updated_at
    BEFORE UPDATE ON oauth_credentials
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_action_tasks_updated_at
    BEFORE UPDATE ON action_tasks
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
