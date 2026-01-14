-- 1) Credenciales WebAuthn
create table if not exists webauthn_credentials (
  id uuid primary key,
  identity_id uuid not null references identities(id) on delete cascade,

  credential_id text not null,
  public_key_cose bytea not null,
  sign_count bigint not null default 0,

  aaguid uuid null,
  transports text null,

  name text null,
  enabled boolean not null default true,

  created_at timestamptz not null,
  last_used_at timestamptz null
);

create unique index if not exists uk_webauthn_credential_id
  on webauthn_credentials (credential_id);

create index if not exists idx_webauthn_identity
  on webauthn_credentials (identity_id);

-- 2) Sesión: marca si requiere MFA y si ya fue verificado
alter table if exists sessions
  add column if not exists mfa_required boolean not null default false;

alter table if exists sessions
  add column if not exists mfa_verified_at timestamptz null;
