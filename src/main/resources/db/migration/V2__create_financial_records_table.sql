-- V2__create_financial_records_table.sql
-- Creates the financial_records table for income/expense entries.
--
-- Design notes:
--   - NUMERIC(19, 4): exact decimal storage — critical for financial data.
--     DOUBLE PRECISION would introduce floating-point rounding errors (e.g., 0.1 + 0.2 ≠ 0.3).
--   - transaction_date is DATE (no time zone) because financial dates are
--     calendar-based; the exact moment within a day is irrelevant for accounting.
--   - deleted flag (soft-delete) preserves audit trail and keeps created_by FKs intact.
--   - created_by references users(id) but is NOT a hard FK — this is intentional:
--     a deleted admin's records remain valid for audit purposes. The UUID is stored
--     for display/filter; we don't cascade or restrict on user deletion.
--   - Composite partial index on (type, category) WHERE deleted = false
--     ensures fast category-sum queries only scan active records.

CREATE TABLE financial_records (
    id               UUID           NOT NULL DEFAULT gen_random_uuid(),
    amount           NUMERIC(19, 4) NOT NULL CHECK (amount > 0),
    type             VARCHAR(10)    NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    category         VARCHAR(100)   NOT NULL,
    transaction_date DATE           NOT NULL,
    notes            VARCHAR(500),
    deleted          BOOLEAN        NOT NULL DEFAULT FALSE,
    created_by       UUID           NOT NULL,   -- Soft-reference to users(id)
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_financial_records PRIMARY KEY (id)
);

-- Partial indexes — only index non-deleted rows for dashboard aggregation queries
CREATE INDEX idx_records_type_active
    ON financial_records (type)
    WHERE deleted = FALSE;

CREATE INDEX idx_records_category_active
    ON financial_records (category, type)
    WHERE deleted = FALSE;

CREATE INDEX idx_records_date_active
    ON financial_records (transaction_date DESC)
    WHERE deleted = FALSE;

-- Composite index for monthly trend queries: date range + type
CREATE INDEX idx_records_date_type
    ON financial_records (transaction_date, type)
    WHERE deleted = FALSE;

COMMENT ON TABLE  financial_records                IS 'Individual income/expense financial entries';
COMMENT ON COLUMN financial_records.amount         IS 'Exact decimal amount. NUMERIC prevents floating-point errors.';
COMMENT ON COLUMN financial_records.type           IS 'INCOME or EXPENSE';
COMMENT ON COLUMN financial_records.transaction_date IS 'Calendar date of the transaction (no time component)';
COMMENT ON COLUMN financial_records.deleted        IS 'Soft-delete flag. True = hidden from standard queries.';
COMMENT ON COLUMN financial_records.created_by     IS 'UUID of the admin who created this record (not a hard FK).';
