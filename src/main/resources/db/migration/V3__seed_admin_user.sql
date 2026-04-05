-- V3__seed_admin_user.sql
-- Credentials:
--   admin@finance-dashboard.com  / Admin@1234
--   analyst@finance-dashboard.com / Analyst@1234
--   viewer@finance-dashboard.com  / Viewer@1234

INSERT INTO users (id, name, email, password_hash, role, active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'System Admin',
    'admin@finance-dashboard.com',
    '$2a$12$9M4Mf/3F5BBpq2/lsZS4F..QElydFyqvzllxyJBkHjXN.mL9vRYSC',
    'ADMIN',
    TRUE,
    NOW(),
    NOW()
);

INSERT INTO users (id, name, email, password_hash, role, active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Finance Analyst',
    'analyst@finance-dashboard.com',
    '$2a$12$9M4Mf/3F5BBpq2/lsZS4F..QElydFyqvzllxyJBkHjXN.mL9vRYSC',
    'ANALYST',
    TRUE,
    NOW(),
    NOW()
);

INSERT INTO users (id, name, email, password_hash, role, active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Dashboard Viewer',
    'viewer@finance-dashboard.com',
    '$2a$12$9M4Mf/3F5BBpq2/lsZS4F..QElydFyqvzllxyJBkHjXN.mL9vRYSC',
    'VIEWER',
    TRUE,
    NOW(),
    NOW()
);