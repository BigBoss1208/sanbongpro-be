-- MANUAL ONLY: do not execute against the real database without owner approval.
-- Target verified read-only: database ra99_n2, table public.thanh_toan.
-- Adds only the entity's refund-handling flag. No VNPay index/logic changes.
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';

ALTER TABLE public.thanh_toan
    ADD COLUMN IF NOT EXISTS can_xu_ly_hoan_tien BOOLEAN NOT NULL DEFAULT FALSE;

COMMIT;
