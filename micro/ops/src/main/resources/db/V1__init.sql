CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- templates
CREATE TABLE IF NOT EXISTS notification_templates (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  channel text NOT NULL CHECK (channel IN ('email','sms','push')),
  name text NOT NULL,
  body text NOT NULL,
  version int NOT NULL DEFAULT 1,
  UNIQUE(channel, name, version)
);

-- prefs
CREATE TABLE IF NOT EXISTS notification_prefs (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_id uuid NOT NULL,
  channel text NOT NULL CHECK (channel IN ('email','sms','push')),
  opt_in boolean NOT NULL,
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(customer_id, channel)
);

-- events queue
CREATE TABLE IF NOT EXISTS notification_events (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  topic text NOT NULL,
  payload_json jsonb NOT NULL,
  channel_override text NULL CHECK (channel_override IN ('email','sms','push')),
  status text NOT NULL DEFAULT 'queued' CHECK (status IN ('queued','sent','failed')),
  retry_count int NOT NULL DEFAULT 0,
  next_retry_at timestamptz NULL,
  last_error text NULL,
  trace_id uuid NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notif_events_due
  ON notification_events(status, next_retry_at, created_at);

-- audit append-only
CREATE TABLE IF NOT EXISTS audit_events (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  actor_type text NOT NULL CHECK (actor_type IN ('user','admin','system')),
  actor_id uuid NULL,
  action text NOT NULL,
  resource text NOT NULL,
  resource_id text NOT NULL,
  ip inet NULL,
  user_agent text NULL,
  trace_id uuid NOT NULL,
  occurred_at timestamptz NOT NULL,
  received_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_trace
  ON audit_events(trace_id, occurred_at DESC);

-- data access logs (ASVS V8/V10)
CREATE TABLE IF NOT EXISTS data_access_logs (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  subject text NOT NULL,
  operation text NOT NULL,
  table_name text NOT NULL,
  record_id uuid NULL,
  purpose text NOT NULL,
  occurred_at timestamptz NOT NULL DEFAULT now()
);
