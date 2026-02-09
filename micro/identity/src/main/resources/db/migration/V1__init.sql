-- =========================================================
-- 0) Extensiones
-- =========================================================
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================================================
-- 1) identities
-- =========================================================
CREATE TABLE IF NOT EXISTS identities (
  id                uuid PRIMARY KEY,
  subject_id_oidc   varchar(255) NOT NULL,
  provider          varchar(64)  NOT NULL,
  user_status       varchar(32)  NOT NULL,
  created_at        timestamptz  NOT NULL,

  CONSTRAINT uk_identities_subject_provider
    UNIQUE (subject_id_oidc, provider)
);

-- Índices útiles (además del UNIQUE)
CREATE INDEX IF NOT EXISTS idx_identities_provider
  ON identities (provider);

CREATE INDEX IF NOT EXISTS idx_identities_user_status
  ON identities (user_status);


-- =========================================================
-- 2) sessions
-- =========================================================
CREATE TABLE IF NOT EXISTS sessions (
  session_id              uuid PRIMARY KEY,
  identity_id             uuid NOT NULL,

  refresh_token_hash      varchar(64) NOT NULL,
  refresh_token_enc       text NOT NULL,

  expires_at              timestamptz NULL,
  created_at              timestamptz NULL,
  revoked_at              timestamptz NULL,
  absolute_expires_at     timestamptz NULL,

  rotated_to_session_id   uuid NULL,

  ip                      text NULL,
  ua                      text NULL,

  CONSTRAINT fk_sessions_identity
    FOREIGN KEY (identity_id) REFERENCES identities(id)
    ON DELETE CASCADE,

  CONSTRAINT fk_sessions_rotated_to
    FOREIGN KEY (rotated_to_session_id) REFERENCES sessions(session_id)
    ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_sessions_identity_id
  ON sessions (identity_id);

CREATE INDEX IF NOT EXISTS idx_sessions_refresh_token_hash
  ON sessions (refresh_token_hash);

CREATE INDEX IF NOT EXISTS idx_sessions_expires_at
  ON sessions (expires_at);


-- =========================================================
-- 3) identity_links (tabla puente identity <-> customer)
-- =========================================================
CREATE TABLE IF NOT EXISTS identity_links (
  identity_id uuid NOT NULL,
  customer_id uuid NOT NULL,

  PRIMARY KEY (identity_id, customer_id),

  CONSTRAINT fk_identity_links_identity
    FOREIGN KEY (identity_id) REFERENCES identities(id)
    ON DELETE CASCADE

  -- Nota: si "customers" vive en otro microservicio, NO pongas FK.
  -- Si sí existe en la misma BD, agrega:
  -- ,CONSTRAINT fk_identity_links_customer
  --   FOREIGN KEY (customer_id) REFERENCES customers(id)
  --   ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_identity_links_customer_id
  ON identity_links (customer_id);


-- =========================================================
-- 4) registration_intents
-- =========================================================
CREATE TABLE IF NOT EXISTS registration_intents (
  id                        uuid PRIMARY KEY,

  email                     text NULL,
  phone                     text NULL,
  channel                   text NULL,
  state                     text NULL,

  national_id               varchar(32) NULL,
  national_id_issue_date    date NULL,
  fingerprint_code          varchar(64) NULL,

  monthly_income            numeric(19,4) NULL,
  occupation_type           varchar(64) NULL,

  created_at                timestamptz NULL,
  updated_at                timestamptz NULL,

  activation_ref            varchar(64) NULL,
  customer_id               varchar(64) NULL,
  primary_account_id        varchar(64) NULL,
  savings_account_id        varchar(64) NULL,
  bonus_journal_id          varchar(64) NULL,

  activated_at              timestamptz NULL,

  CONSTRAINT uq_registration_activation_ref
    UNIQUE (activation_ref)
);

-- Indexes típicos para onboarding/consultas operativas
CREATE INDEX IF NOT EXISTS idx_registration_state
  ON registration_intents (state);

CREATE INDEX IF NOT EXISTS idx_registration_email
  ON registration_intents (email);

CREATE INDEX IF NOT EXISTS idx_registration_phone
  ON registration_intents (phone);

CREATE INDEX IF NOT EXISTS idx_registration_created_at
  ON registration_intents (created_at);


-- =========================================================
-- 5) registration_kyc_objects
-- =========================================================
-- NOTA: kind depende de tu enum KycDocumentKind; aquí lo dejamos como varchar(32)
-- y stage como varchar(16) con CHECK.
CREATE TABLE IF NOT EXISTS registration_kyc_objects (
  id               bigserial PRIMARY KEY,

  registration_id  uuid NOT NULL,

  kind             varchar(32) NOT NULL,
  stage            varchar(16) NOT NULL,

  bucket           varchar(128)  NOT NULL,
  object_key       varchar(1024) NOT NULL,

  etag             varchar(128) NULL,
  content_type     varchar(128) NULL,
  content_length   bigint NULL,

  expires_at       timestamptz NULL,
  max_bytes        bigint NULL,

  created_at       timestamptz NOT NULL,
  updated_at       timestamptz NOT NULL,

  CONSTRAINT fk_kyc_registration
    FOREIGN KEY (registration_id) REFERENCES registration_intents(id)
    ON DELETE CASCADE,

  CONSTRAINT uq_reg_kind_stage
    UNIQUE (registration_id, kind, stage),

  CONSTRAINT chk_kyc_stage
    CHECK (stage IN ('STAGING', 'FINAL'))
);

CREATE INDEX IF NOT EXISTS idx_kyc_registration_id
  ON registration_kyc_objects (registration_id);

CREATE INDEX IF NOT EXISTS idx_kyc_bucket_key
  ON registration_kyc_objects (bucket, object_key);
