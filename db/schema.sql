-- NextGenBank schema — run once against a fresh PostgreSQL database, e.g.:
--   createdb nextgenbank
--   psql -d nextgenbank -f schema.sql
--   psql -d nextgenbank -f seed.sql

CREATE TABLE customers (
    customer_id     BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50) UNIQUE NOT NULL,
    password_hash   VARCHAR(64) NOT NULL,      -- SHA-256 hex, see PasswordUtil.java
    full_name       VARCHAR(120) NOT NULL,
    email           VARCHAR(120),
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE accounts (
    account_id      BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT NOT NULL REFERENCES customers(customer_id),
    account_number  VARCHAR(20) UNIQUE NOT NULL,
    account_type    VARCHAR(20) NOT NULL CHECK (account_type IN ('CHECKING','SAVINGS')),
    balance         NUMERIC(14,2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE transactions (
    transaction_id   BIGSERIAL PRIMARY KEY,
    from_account_id  BIGINT REFERENCES accounts(account_id),
    to_account_id    BIGINT REFERENCES accounts(account_id),
    amount           NUMERIC(14,2) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',   -- PENDING, COMPLETED, FAILED
    created_at       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE audit_log (
    audit_id         BIGSERIAL PRIMARY KEY,
    transaction_id   BIGINT REFERENCES transactions(transaction_id),
    event_detail     TEXT NOT NULL,
    logged_at        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_accounts_customer ON accounts(customer_id);
CREATE INDEX idx_transactions_from ON transactions(from_account_id);
CREATE INDEX idx_transactions_to   ON transactions(to_account_id);
