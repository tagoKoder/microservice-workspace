-- Se ejecuta SOLO en el primer arranque (cuando pgdata está vacío)

CREATE DATABASE identity;
CREATE DATABASE account;
CREATE DATABASE ledger;
CREATE DATABASE bff;

-- opcional: extensions por DB (si usas gen_random_uuid() en ledger)
\connect ledger
CREATE EXTENSION IF NOT EXISTS pgcrypto;

\connect account
CREATE EXTENSION IF NOT EXISTS pgcrypto;

\connect identity
CREATE EXTENSION IF NOT EXISTS pgcrypto;

\connect bff
CREATE EXTENSION IF NOT EXISTS pgcrypto;
