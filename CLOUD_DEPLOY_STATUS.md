# Tiến độ cloud — 20/10/2026

- Phạm vi chỉ hai repository cá nhân, không sử dụng hạ tầng/credential thầy hoặc GitLab nhóm.
- Đã xác minh Neon account do người dùng cung cấp, Owned project sweet-heart-82392879, branch production br-lucky-voice-b5rippec, database QuanLiSanBong, role neondb_owner.
- Query trước baseline: public 0 bảng. Đã chạy đúng database/baseline/neon_schema.sql qua SQL Editor; COMMIT thành công.
- Query sau baseline: 9 bảng, 10 foreign keys, 23 index; users/bookings/payments đều 0. Không seed hoặc sao chép dữ liệu cũ.
- Người dùng đã duyệt seed demo, nhưng chưa có mật khẩu ADMIN nhập riêng nên chưa chạy seed.
- Render: người dùng xác nhận workspace cá nhân, chọn Free, GMAIL_ENABLED=false; form tạo sanbongpro-be từ GitHub cá nhân đã chuẩn bị, health /api/health, auto-deploy Off. Chưa tạo/deploy vì secrets còn thiếu.
- Đã điền JDBC direct Neon với verify-full vào form Render; không lưu password trong source.
- Cloudflare: tài khoản cá nhân đã đăng nhập; Pages GitHub App hiện chỉ thấy quiz_app, cần thêm quyền riêng sanbongpro-fe. Người dùng đã đồng ý; GitHub đang yêu cầu email verification. Chưa tạo Pages.
- Source FE đã bỏ fallback URL ảnh về VPS cũ; khi thiếu kho riêng chỉ hiển thị placeholder. Backend Return URL và bucket yêu cầu cấu hình rõ ràng. Không đổi UI.
- Cần người dùng nhập DB password, JWT secret mới, MinIO riêng, VNPay Sandbox riêng trực tiếp trong Render. Không đưa secret vào chat/log/Git.
- Gmail chưa hoạt động trên Free theo lựa chọn người dùng. Chưa có URL website/API live và chưa kiểm thử luồng đặt sân/thanh toán trên cloud.
