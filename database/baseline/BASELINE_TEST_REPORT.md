# Kết quả kiểm thử baseline — 20/10/2026

## Đã kiểm thử PostgreSQL thật

- PostgreSQL local 18.6, cluster mới hoàn toàn trong `target/baseline-pg-4f56690bf798`.
- Chỉ nghe `127.0.0.1:62714`, database riêng `qlsb_baseline_test_4f56690bf798`; password test ngẫu nhiên, SCRAM. Không kết nối Neon hoặc VPS thầy.
- Baseline thực thi thành công: 9 bảng, 9 PK identity, 10 FK, 5 unique nghiệp vụ; tổng 23 index = 9 PK + 5 unique + 9 index đọc.
- Chạy cả hai migration 20261012/20261013 sau baseline và sau seed: số index không tăng, dữ liệu seed không thay đổi.
- Chạy baseline lần hai bị từ chối vì public đã có bảng, không xóa/rewrite dữ liệu.
- Seed thiếu input hoặc hash không hợp lệ bị từ chối trước insert; seed hợp lệ tạo 1 ADMIN hoạt động, 1 cụm, 1 sân, 3 khung giờ, 3 giá; không có đơn/thanh toán. Seed lần hai bị từ chối, không sửa tài khoản cũ.
- Cluster kiểm thử đã dừng; test password/hash tạm đã xóa. Log và dữ liệu cluster giữ trong target, không được Git theo dõi.

## Hibernate và kiểm tra mapping

`PostgresBaselineTest`: 1 test thật, 0 failure/error/skip, Maven BUILD SUCCESS.

- Hibernate project (5.6, PostgreSQL10Dialect) dùng hbm2ddl.auto=validate; không create/update hoặc tự sửa schema.
- Đối chiếu chính xác tập cột của từng entity, SQL type, độ dài VARCHAR, precision/scale NUMERIC, nullable và identity qua information_schema; Hibernate validate bổ sung đối chiếu mapping.
- SQL từ chối FK không hợp lệ (23503), NOT NULL (23502), username trùng (23505).
- Ghi/đọc cả 9 entity trong transaction được rollback; ActiveStatusConverter xác nhận active → khoa_tai_khoan=false và locked → true, rồi load lại đúng trạng thái Java.
- Ghi/đọc toàn bộ giá trị của VaiTro, TrangThaiDatSan, TrangThaiThanhToan và PhuongThucThanhToan.
- DatSan.@PrePersist vẫn tạo GIU_CHO_TAM/ngay_tao; GiaSan Java default TAT_CA; refund flag false. Không tự biến các Java default thành SQL DEFAULT ngoài source.
- Đây là kiểm tra Hibernate EntityManagerFactory và mapping, không phải xác nhận Render đã khởi động toàn bộ service.

## Bộ test/build Backend

Maven offline verify với profile test: BUILD SUCCESS; 52 test được phát hiện, 51 chạy đạt, 1 PostgreSQL test được skip có chủ đích vì yêu cầu runner loopback riêng. Test PostgreSQL đó đã chạy riêng và đạt như phần trên. Không có failure/error.

## File chuẩn bị

- database/baseline/neon_schema.sql
- database/baseline/neon_seed.sql
- database/baseline/README.md
- src/test/java/com/example/becicd/PostgresBaselineTest.java
- scripts/test_neon_baseline.py
- CLOUD_SETUP.md cập nhật đường dẫn baseline mới.
- .gitignore bổ sung Python cache; không sửa Java nghiệp vụ, UI, hai migration cũ hoặc ddl-auto cloud.

## Chưa xác minh / cần duyệt

- Chưa thực thi SQL trên Neon, chưa kiểm tra database QuanLiSanBong, quyền role neondb_owner, SSL hoặc kết nối từ Render.
- Giá và thông tin sân trong seed là mẫu mới, cần duyệt trước khi mở đặt sân thật. Mật khẩu ADMIN phải tự tạo và truyền qua BCrypt hash riêng tư; không có credential triển khai mặc định.
- Không copy bất kỳ dữ liệu người dùng/đặt sân/thanh toán nào từ VPS.
- Enum lưu VARCHAR giống source, không thêm CHECK; các index đọc bổ sung chưa benchmark trên dữ liệu production.
- Chưa push/commit các thay đổi hoặc deploy. Không chạy lại migration cũ trên Neon mới nếu baseline đã bao gồm chúng.
