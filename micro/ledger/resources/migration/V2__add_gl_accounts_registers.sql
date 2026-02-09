INSERT INTO gl_accounts (code, name, type)
VALUES
  ('1000', 'Cash - Main', 'asset'),
  ('1100', 'Settlement - Clearing Asset', 'asset'),
  ('2000', 'Customer Deposits', 'liability'),
  ('2100', 'Transfers Clearing', 'liability'),
  ('4000', 'Transfer Fees Income', 'income'),
  ('5000', 'Payment Processing Expense', 'expense'),
  ('3000', 'Equity', 'equity')
ON CONFLICT (code) DO NOTHING;

INSERT INTO gl_accounts (id, code, name, type)
VALUES
  ('11111111-1111-1111-1111-111111111111', 'GL_SYSTEM_FUND',  'System funding source', 'asset'),
  ('22222222-2222-2222-2222-222222222222', 'GL_CUSTOMER_CASH', 'Customer cash subledger', 'liability'),
  ('33333333-3333-3333-3333-333333333333', 'GL_OUT',          'Transfer out', 'liability'),
  ('44444444-4444-4444-4444-444444444444', 'GL_IN',           'Transfer in', 'liability')
ON CONFLICT (code) DO NOTHING;
