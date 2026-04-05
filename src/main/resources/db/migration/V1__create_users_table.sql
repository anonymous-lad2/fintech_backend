-- V1__create_users_table.sql
-- Creates the core users table with role-based access control support.
--
-- Design notes:
--   - UUID primary key: avoids enumerable integer IDs in REST URLs and
--     is safe for distributed ID generation without a central sequence.
--   - email UNIQUE constraint enforced at DB level (not just application level)
--     to prevent race conditions on concurrent registration.
--   - active flag enables soft-delete; deactivated users preserve FK integrity
--     with financial records.
--   - BCrypt hashes are always 60 chars; 255 provides future-proofing.

CREATE TABLE users (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    name         VARCHAR(100) NOT NULL,
    email        VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role         VARCHAR(20)  NOT NULL CHECK (role IN ('VIEWER', 'ANALYST', 'ADMIN')),
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_users          PRIMARY KEY (id),
    CONSTRAINT uq_users_email    UNIQUE (email)
);

-- Index for login lookup (most frequent query)
CREATE INDEX idx_users_email_active ON users (email) WHERE active = TRUE;
-- Index for admin role-filter listing
CREATE INDEX idx_users_role        ON users (role);

COMMENT ON TABLE  users              IS 'System users with role-based dashboard access';
COMMENT ON COLUMN users.password_hash IS 'BCrypt-hashed password, strength 12. Never stored in plain text.';
COMMENT ON COLUMN users.active        IS 'Soft-delete flag. False = deactivated, login blocked.';
COMMENT ON COLUMN users.role          IS 'VIEWER: read-only | ANALYST: analytics | ADMIN: full access';
