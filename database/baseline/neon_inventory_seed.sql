-- Personal Neon inventory only, approved demo. Never modifies existing inventory.
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';
DO $$ BEGIN
 IF current_database() <> 'QuanLiSanBong' THEN RAISE EXCEPTION 'Wrong database'; END IF;
 IF EXISTS(SELECT 1 FROM public.cum_san) OR EXISTS(SELECT 1 FROM public.san)
 OR EXISTS(SELECT 1 FROM public.khung_gio) OR EXISTS(SELECT 1 FROM public.gia_san)
 THEN RAISE EXCEPTION 'Inventory is not empty; no existing data changed'; END IF;
END $$;
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
