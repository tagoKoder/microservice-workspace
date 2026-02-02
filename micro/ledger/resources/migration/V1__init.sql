-- Requerido para gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================
-- GL Accounts
-- =========================
CREATE TABLE IF NOT EXISTS gl_accounts (
  id   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(20) UNIQUE NOT NULL,
  name text NOT NULL,
  type text NOT NULL CHECK (type IN ('asset','liability','income','expense','equity'))
);

-- =========================
-- Journal
-- =========================
CREATE TABLE IF NOT EXISTS journal_entries (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  external_ref text,
  booked_at    timestamptz NOT NULL,
  created_by   text NOT NULL,
  status       text NOT NULL CHECK (status IN ('posted','reversed','void')),
  currency     char(3) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_journal_entries_booked_at ON journal_entries(booked_at);

CREATE TABLE IF NOT EXISTS entry_lines (
  id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  journal_id        uuid NOT NULL REFERENCES journal_entries(id) ON DELETE CASCADE,
  gl_account_id     uuid NOT NULL REFERENCES gl_accounts(id),
  gl_account_code   varchar(20) NOT NULL,
  counterparty_ref  text NULL,
  debit             numeric(20,6) NOT NULL DEFAULT 0,
  credit            numeric(20,6) NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_entry_lines_journal_id ON entry_lines(journal_id);
CREATE INDEX IF NOT EXISTS idx_entry_lines_gl_account_id ON entry_lines(gl_account_id);

-- =========================
-- Payments (alineado con tu entity actual)
-- =========================
CREATE TABLE IF NOT EXISTS payments (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  idempotency_key varchar(255) UNIQUE NOT NULL,
  source_account  uuid NOT NULL,
  dest_account    uuid NOT NULL,
  amount          numeric(20,6) NOT NULL,
  currency        char(3) NOT NULL,
  hold_id uuid not null,
  journal_id uuid null,
  correlation_id varchar(128) null,
  updated_at timestamptz not null,
  status          text NOT NULL CHECK (status IN ('pending','processing','posted','failed')),
  customer_id     uuid NOT NULL,
  created_at      timestamptz NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_payments_source_account ON payments(source_account);
CREATE INDEX IF NOT EXISTS idx_payments_dest_account   ON payments(dest_account);
CREATE INDEX IF NOT EXISTS idx_payments_customer_id    ON payments(customer_id);

CREATE TABLE IF NOT EXISTS payment_steps (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  payment_id   uuid NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
  step         text NOT NULL,
  state        text NOT NULL,
  details      jsonb NOT NULL DEFAULT '{}'::jsonb,
  attempted_at timestamptz NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_payment_steps_payment_id ON payment_steps(payment_id);

-- =========================
-- Outbox (alineado con tu código de claim)
-- =========================
CREATE TABLE IF NOT EXISTS outbox_events (
  id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  aggregate_type   text NOT NULL,
  aggregate_id     uuid NOT NULL,
  event_type       text NOT NULL,
  payload          jsonb NOT NULL,
  published        boolean NOT NULL DEFAULT false,
  published_at     timestamptz NULL,

  processing       boolean NOT NULL DEFAULT false,
  processing_owner text NULL,
  processing_at    timestamptz NULL,

  created_at       timestamptz NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_outbox_published_created
  ON outbox_events(published, created_at);

CREATE INDEX IF NOT EXISTS idx_outbox_processing_created
  ON outbox_events(processing, created_at);

CREATE INDEX IF NOT EXISTS idx_outbox_event_type
  ON outbox_events(event_type);

-- =========================
-- Idempotency
-- =========================
CREATE TABLE IF NOT EXISTS idempotency_records (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  key           varchar(255) UNIQUE NOT NULL,
  operation     text NOT NULL,
  response_json jsonb NOT NULL,
  status_code   integer NOT NULL,
  created_at    timestamptz NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_idempotency_created_at
  ON idempotency_records(created_at);
