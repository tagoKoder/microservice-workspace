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
