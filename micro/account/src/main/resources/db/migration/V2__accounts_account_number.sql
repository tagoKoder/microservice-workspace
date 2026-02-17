-- 1) sequence
create sequence if not exists account_number_seq
  increment by 1
  minvalue 11234
  start with 11234
  maxvalue 999999999999
  cache 50;

-- 2) column (nullable primero para backfill)
alter table accounts
  add column if not exists account_number bigint;

-- 3) backfill existentes (si ya hay cuentas creadas)
update accounts
set account_number = nextval('account_number_seq')
where account_number is null;

-- 4) default + not null
alter table accounts
  alter column account_number set default nextval('account_number_seq'),
  alter column account_number set not null;

-- 5) unique
create unique index if not exists uq_accounts_account_number on accounts(account_number);

-- 6) ajustar secuencia para que el siguiente nextval sea:
--    max(account_number)+1, o 11234 si no hay registros
select setval(
  'account_number_seq',
  (select greatest(coalesce(max(account_number), 11234), 11234) from accounts),
  false
);


