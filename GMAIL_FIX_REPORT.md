# Kiểm tra và sửa Gmail xác nhận VNPay

## Nguyên nhân xác định từ cấu hình

Ban đầu GMAIL_ENABLED mặc định false, GMAIL_USERNAME và GMAIL_APP_PASSWORD chưa có trong IntelliJ hoặc môi trường. Code khi disabled return ngay không ghi lý do. Đơn #33 đã DA_XAC_NHAN, có một THANH_CONG và có email người nhận (kiểm tra PostgreSQL read-only, không in thông tin cá nhân). Sự kiện email chỉ phát ở lần chuyển trạng thái thành công, nên cấu hình SMTP sau đó không tự gửi bù đơn đã thanh toán. Không có ledger email để khẳng định mọi lần gửi trước đó.

## Sửa code Backend local

PaymentEmailService.java: thêm chẩn đoán cấu hình khi startup, log rõ bỏ qua khi disabled, kiểm tra username/App Password/người nhận trước gửi, log accepted by SMTP, phân loại lỗi xác thực/timeout/kết nối. Không log credential, email người nhận hoặc exception message/stack trace thô. Giữ AFTER_COMMIT và bắt lỗi để không rollback thanh toán.

BookingIntegrationTest.java: cấu hình App Password giả lập đúng cho mail mock.
PaymentEmailServiceTest.java mới: kiểm tra disabled/thiếu password/thiếu người nhận, lỗi xác thực không lan ra, nội dung chứa khách/sân/ngày giờ/tiền/mã đơn.
HUONG_DAN_GMAIL.md cập nhật cách cấu hình và chẩn đoán.

Không đổi luồng xác thực VNPay, UI, schema hoặc dữ liệu thanh toán. Chống trùng vẫn dựa vào khóa đơn và chuyển trạng thái thanh toán, dùng chung Return/IPN. Không tự gửi lại khi callback lặp hoặc retry SMTP.

## Kết quả

- Maven offline clean package: BUILD SUCCESS, 51 tests, 0 failure/error/skip.
- Gmail SMTP 587 + STARTTLS: PASS; certificate verification bật.
- AUTH thực tế: 235.
- Gửi một email thử không chứa dữ liệu khách hàng vào Gmail gửi thư đã cấu hình: Gmail chấp nhận. Người dùng xác nhận đã nhận mail.
- Không gửi bù đơn #33 vì người dùng báo đã nhận mail, tránh trùng.
- Lần build incremental đầu lỗi class thiếu; clean build đã khắc phục và toàn bộ test đạt.
- Chưa chạy lại một giao dịch VNPay ngân hàng thật mới với email tự động trên tiến trình Backend đã restart; test VNPay/callback và SMTP thật được xác minh riêng.

## Kiểm tra Gmail sau sửa

Restart Backend IntelliJ bằng cấu hình có GMAIL_ENABLED=true, GMAIL_USERNAME, GMAIL_APP_PASSWORD. Dùng Customer của mình có email thật và thanh toán một đơn Sandbox mới. Sau Backend xác nhận, tìm log accepted by SMTP theo mã đơn, kiểm tra Inbox/Spam. Gọi lặp callback phải không có lần gửi thứ hai. Không tạo đơn giả vào DB thật để thử.

Không commit, push, tạo MR hoặc deploy VPS.
