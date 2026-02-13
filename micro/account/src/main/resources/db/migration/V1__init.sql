create extension if not exists pgcrypto;

-- customers
create table if not exists customers (
  id uuid primary key default gen_random_uuid(),
  full_name text not null,
  birth_date date not null,
  tin text not null,
  risk_segment text not null check (risk_segment in ('low','medium','high')),
  status text not null check (status in ('active','suspended')),
  kyc_level text null,
  kyc_verified_at timestamptz null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists customer_contacts (
  id bigserial primary key,
  customer_id uuid not null references customers(id) on delete cascade,
  email text null,
  email_verified boolean not null default false,
  phone text null
);

create table if not exists preferences (
  id bigserial primary key,
  customer_id uuid not null references customers(id) on delete cascade,
  channel text not null check (channel in ('email','sms','push')),
  opt_in boolean not null default true,
  unique (customer_id)
);

create table if not exists customer_addresses (
  id bigserial primary key,
  customer_id uuid not null references customers(id) on delete cascade,
  country varchar(2) not null,
  line1 text not null,
  line2 text null,
  city text not null,
  province text null,
  postal_code text null,
  verified boolean not null default false
);

-- accounts
create table if not exists accounts (
  id uuid primary key default gen_random_uuid(),
  customer_id uuid not null references customers(id),
  product_type text not null check (product_type in ('checking','savings')),
  currency varchar(3) not null,
  status text not null check (status in ('active','frozen')),
  opened_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_accounts_customer on accounts(customer_id);

create table if not exists account_balances (
  account_id uuid primary key references accounts(id) on delete cascade,
  ledger numeric(20,6) not null default 0,
  available numeric(20,6) not null default 0,
  hold numeric(20,6) not null default 0
);

create table if not exists account_limits (
  account_id uuid primary key references accounts(id) on delete cascade,
  daily_out numeric(20,6) not null default 0,
  daily_in numeric(20,6) not null default 0
);

-- HOLDS (obligatorio por tu proto)
create table if not exists account_holds (
  account_id uuid not null references accounts(id) on delete cascade,
  hold_id uuid not null,
  currency varchar(3) not null,
  amount numeric(20,6) not null check (amount > 0),
  status text not null check (status in ('reserved','released','settled')),
  idempotency_key varchar(255) not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  primary key (account_id, hold_id)
);

create unique index if not exists uq_account_holds_idk on account_holds(idempotency_key);

-- Idempotency genérica (CreateCustomer/CreateAccount/ReserveHold/ReleaseHold)
create table if not exists idempotency_records (
  id uuid primary key default gen_random_uuid(),
  idempotency_key varchar(255) not null unique,
  operation text not null,
  response_json jsonb not null,
  status_code int not null,
  created_at timestamptz not null default now()
);

-- Inbox para eventos (ledger.journal.posted)
create table if not exists inbox_events (
  event_id varchar(128) primary key,
  event_type text not null,
  received_at timestamptz not null default now(),
  processed_at timestamptz null,
  status text not null default 'received',
  error text null
);

create table if not exists account_opening_bonus_grants (
  idempotency_key text primary key,
  account_id uuid not null,
  journal_id text not null,
  amount numeric(20,6) not null,
  currency char(3) not null,
  created_at timestamptz not null default now()
);