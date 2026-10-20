-- MANUAL REVIEW ONLY. This file is not automatically executed.
-- Take a backup and review deployment against a staging copy first.
BEGIN;
ALTER TABLE thanh_toan ADD COLUMN IF NOT EXISTS can_xu_ly_hoan_tien boolean NOT NULL DEFAULT false;
-- Preserve existing rows: duplicates require investigation, never silently delete/rewrite refs.
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM thanh_toan WHERE ma_giao_dich_vnpay IS NOT NULL GROUP BY ma_giao_dich_vnpay HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'Duplicate VNPay references require manual investigation';
    END IF;
END $$;
CREATE UNIQUE INDEX IF NOT EXISTS ux_thanh_toan_vnpay_reference ON thanh_toan (ma_giao_dich_vnpay);
COMMIT;
