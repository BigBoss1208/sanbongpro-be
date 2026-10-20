# Gmail xác nhận thanh toán VNPay — chỉ sửa local

## Cấu hình IntelliJ

Edit Configurations → BeCicdApplication → Environment variables:

```properties
GMAIL_ENABLED=true
GMAIL_USERNAME=<địa chỉ Gmail gửi thư>
GMAIL_APP_PASSWORD=<App Password do Google cấp, bỏ khoảng trắng phân nhóm>
```

Không dùng mật khẩu đăng nhập Google, không gửi App Password vào chat/Git. Không ghi giá trị thật trong application.yml hoặc .env.example. Nếu chưa muốn gửi, giữ GMAIL_ENABLED=false (mặc định); Backend và thanh toán vẫn hoạt động.

## Lấy App Password

1. Bật Xác minh 2 bước cho tài khoản Google.
2. Vào https://myaccount.google.com/apppasswords, tạo App Password riêng cho SÂN BÓNG PRO.
3. Điền vào biến môi trường trên máy; restart Backend.

Nếu không thấy tùy chọn, tài khoản cơ quan/trường học có thể bị quản trị viên hạn chế, chỉ dùng security key hoặc bật Advanced Protection. Tham khảo https://support.google.com/mail/answer/185833 .

Cấu hình source: smtp.gmail.com, 587, STARTTLS bắt buộc, xác thực SMTP, UTF-8; timeout kết nối/đọc/ghi mỗi loại 5 giây. Nguồn SMTP: https://support.google.com/mail/answer/7104828 . Không cần bật POP/IMAP chỉ để gửi email.

## Thử thực tế

Dùng Customer có email thật của bạn. Đặt sân VNPay và hoàn tất giao dịch Sandbox. Sau khi Backend xác thực chữ ký, số tiền, thời hạn và commit THANH_CONG/DA_XAC_NHAN, kiểm tra Inbox/Spam. Callback lặp Return/IPN không gửi lần nữa. Giao dịch thất bại, sai chữ ký, hết hạn hoặc cần xử lý hoàn tiền không gửi thư xác nhận thành công.

Thử sai App Password: thanh toán vẫn phải thành công; log chỉ báo booking ID và loại lỗi, không ghi SMTP password/địa chỉ người nhận. Không tạo API gửi mail thử công khai hoặc thay đổi Database.

## Cách triển khai và giới hạn

VNPayService phát sự kiện snapshot tại duy nhất nhánh chuyển giao dịch sang thành công. PaymentEmailService nghe AFTER_COMMIT và gửi SimpleMailMessage. Nội dung gồm tên khách, sân, ngày/giờ, tổng tiền và thông báo thành công.

Không đổi schema, UI, logic giữ chỗ/hoàn tiền. Chống gửi trùng dựa trên trạng thái thanh toán và khóa đơn sẵn có. Đây là một lần thử gửi, không có hàng đợi hoặc retry: nếu SMTP lỗi hoặc ứng dụng dừng sau commit trước khi gửi, email có thể không được nhận; callback lặp không tự gửi lại. SMTP có thể làm callback chờ tới các timeout đã đặt nhưng không làm rollback thanh toán.

Đã kiểm tra SMTP Gmail thật: STARTTLS thành công, AUTH 235 và Gmail chấp nhận email thử gửi vào chính hộp thư gửi; người dùng xác nhận đã nhận mail. Test tự động dùng JavaMailSender mock. Không commit, push hoặc tạo Merge Request theo yêu cầu.

## Chẩn đoán đơn #33

Kiểm tra read-only PostgreSQL: đơn DA_XAC_NHAN, 1 giao dịch THANH_CONG, có email người nhận. Cấu hình trước kiểm tra không có GMAIL_ENABLED/USERNAME/APP_PASSWORD nên mail bị tắt và code cũ bỏ qua im lặng. Không có lịch sử gửi email trong DB để chứng minh chính xác trạng thái SMTP của mọi lần chạy trước.

Sau khi người dùng thêm các biến trong IntelliJ, SMTP xác thực thành công và email thử được chấp nhận; người dùng xác nhận nhận được mail. Không gửi bù đơn #33 để tránh trùng. Restart Backend bằng Run Configuration đã cập nhật để nạp code/env; không deploy VPS.

Log hiện phân biệt: bị tắt, thiếu cấu hình/người nhận, SMTP_AUTH_FAILED, SMTP_TIMEOUT, SMTP_CONNECTION_FAILED và accepted by SMTP. Không log password, địa chỉ email hoặc thông điệp exception thô. Accepted by SMTP không tự chứng minh mail nằm trong Inbox; kiểm tra cả Spam.

Bật Gmail sau khi đơn đã thành công không tự replay sự kiện cũ. Callback lặp không gửi lại; không thêm retry/hàng đợi/schema hoặc endpoint gửi mail.
