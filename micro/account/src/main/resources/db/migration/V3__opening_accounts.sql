ALTER TABLE account_opening_bonus_grants
  ADD COLUMN IF NOT EXISTS status varchar(16) NOT NULL DEFAULT 'PENDING';

ALTER TABLE account_opening_bonus_grants
  ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now();

ALTER TABLE account_opening_bonus_grants
  ADD COLUMN IF NOT EXISTS locked_at timestamptz;

ALTER TABLE account_opening_bonus_grants
  ADD COLUMN IF NOT EXISTS locked_by text;

ALTER TABLE account_opening_bonus_grants
  ALTER COLUMN account_id DROP NOT NULL;

ALTER TABLE account_opening_bonus_grants
  ALTER COLUMN journal_id DROP NOT NULL;