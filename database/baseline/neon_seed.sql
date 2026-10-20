-- psql 15+ only, NOT Neon SQL Editor syntax. Manual review/approval required.
-- Supply SEED_ADMIN_USERNAME, SEED_ADMIN_EMAIL, SEED_ADMIN_NAME and
-- SEED_ADMIN_PASSWORD_HASH in the process environment, never as CLI arguments.
-- PASSWORD_HASH must be BCrypt generated locally with Spring BCryptPasswordEncoder.
-- No plaintext password, fixed password or hardcoded BCrypt hash in this file.
\set ON_ERROR_STOP on
\getenv admin_username SEED_ADMIN_USERNAME
\getenv admin_email SEED_ADMIN_EMAIL
\getenv admin_name SEED_ADMIN_NAME
\getenv admin_password_hash SEED_ADMIN_PASSWORD_HASH
\if :{?admin_username}
\else
  DO $$ BEGIN RAISE EXCEPTION 'Missing SEED_ADMIN_USERNAME'; END $$;
\endif
\if :{?admin_email}
\else
  DO $$ BEGIN RAISE EXCEPTION 'Missing SEED_ADMIN_EMAIL'; END $$;
\endif
\if :{?admin_name}
\else
  DO $$ BEGIN RAISE EXCEPTION 'Missing SEED_ADMIN_NAME'; END $$;
\endif
\if :{?admin_password_hash}
\else
  DO $$ BEGIN RAISE EXCEPTION 'Missing SEED_ADMIN_PASSWORD_HASH'; END $$;
\endif

SELECT length(:'admin_username') BETWEEN 1 AND 50
   AND length(:'admin_email') BETWEEN 3 AND 100
   AND position('@' IN :'admin_email') > 1
   AND length(:'admin_name') BETWEEN 1 AND 100
   AND :'admin_password_hash' ~ '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$' AS seed_input_valid \gset
\if :seed_input_valid
\else
  DO $$ BEGIN RAISE EXCEPTION 'Invalid seed input (BCrypt hash and valid identity fields required)'; END $$;
\endif

BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';
-- Intentionally one-shot for an empty database. Never modify existing users/data.
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM public.nguoi_dung)
       OR EXISTS (SELECT 1 FROM public.cum_san)
       OR EXISTS (SELECT 1 FROM public.san)
       OR EXISTS (SELECT 1 FROM public.khung_gio)
       OR EXISTS (SELECT 1 FROM public.gia_san)
       OR EXISTS (SELECT 1 FROM public.dat_san)
       OR EXISTS (SELECT 1 FROM public.thanh_toan)
       OR EXISTS (SELECT 1 FROM public.san_yeu_thich)
       OR EXISTS (SELECT 1 FROM public.products) THEN
        RAISE EXCEPTION 'Seed requires empty application tables; no existing rows changed';
    END IF;
END $$;

INSERT INTO public.nguoi_dung (username,email,password,ho_ten,role,khoa_tai_khoan)
VALUES (:'admin_username',:'admin_email',:'admin_password_hash',:'admin_name','ADMIN',FALSE);

-- Minimal sample inventory for review, not copied from VPS or customer records.
WITH cluster AS (
    INSERT INTO public.cum_san (ten_cum,dia_chi,mo_ta)
    VALUES ('Cụm sân Sân Bóng Pro',NULL,'Dữ liệu khởi tạo mới; cập nhật địa chỉ trước khi mở đặt sân')
    RETURNING id
)
INSERT INTO public.san (ten_san,loai_san,mo_ta,trang_thai,cum_san_id)
SELECT 'Sân Pro 5','Sân 5','Sân khởi tạo; giá mẫu cần duyệt','HOAT_DONG',id FROM cluster;

INSERT INTO public.khung_gio (gio_bat_dau,gio_ket_thuc)
VALUES ('17:00','18:30'),('18:30','20:00'),('20:00','21:30');

INSERT INTO public.gia_san (san_id,khung_gio_id,gia,loai_ngay)
SELECT s.id,k.id,300000,'TAT_CA'
FROM public.san s CROSS JOIN public.khung_gio k;
COMMIT;
