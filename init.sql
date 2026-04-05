CREATE TABLE IF NOT EXISTS users (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    name         VARCHAR(100) NOT NULL,
    email        VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role         VARCHAR(20)  NOT NULL CHECK (role IN ('VIEWER', 'ANALYST', 'ADMIN')),
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS financial_records (
    id               UUID           NOT NULL DEFAULT gen_random_uuid(),
    amount           NUMERIC(19, 4) NOT NULL CHECK (amount > 0),
    type             VARCHAR(10)    NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    category         VARCHAR(100)   NOT NULL,
    transaction_date DATE           NOT NULL,
    notes            VARCHAR(500),
    deleted          BOOLEAN        NOT NULL DEFAULT FALSE,
    created_by       UUID           NOT NULL,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_financial_records PRIMARY KEY (id)
);

INSERT INTO users (id, name, email, password_hash, role, active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'System Admin',
    'admin@finance-dashboard.com',
    '$2a$12$9M4Mf/3F5BBpq2/lsZS4F..QElydFyqvzllxyJBkHjXN.mL9vRYSC',
    'ADMIN', TRUE, NOW(), NOW()
) ON CONFLICT (email) DO NOTHING;

INSERT INTO users (id, name, email, password_hash, role, active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Finance Analyst',
    'analyst@finance-dashboard.com',
    '$2a$12$9M4Mf/3F5BBpq2/lsZS4F..QElydFyqvzllxyJBkHjXN.mL9vRYSC',
    'ANALYST', TRUE, NOW(), NOW()
) ON CONFLICT (email) DO NOTHING;

INSERT INTO users (id, name, email, password_hash, role, active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Dashboard Viewer',
    'viewer@finance-dashboard.com',
    '$2a$12$9M4Mf/3F5BBpq2/lsZS4F..QElydFyqvzllxyJBkHjXN.mL9vRYSC',
    'VIEWER', TRUE, NOW(), NOW()
) ON CONFLICT (email) DO NOTHING;