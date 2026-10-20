# Báo cáo sửa MinIO và VNPay — 13/10/2026

## Kết quả và giới hạn

Đã sửa trực tiếp Backend và Frontend. Không commit/push. Không thay credential, không thay schema hoặc ghi dữ liệu thanh toán thật trong quá trình kiểm thử. `ddl-auto=validate` được giữ nguyên cho PostgreSQL.

- Backend: Maven offline `package` BUILD SUCCESS, 45 test, 0 lỗi.
- Frontend: Vite build PASS, ESLint 0 lỗi, 21 test PASS. Chạy trực tiếp CLI trong node_modules do npm global của máy từng lỗi phụ thuộc; cùng các công cụ khai báo trong package.json.
- Giao diện kết quả: mở thật trên Vite 3004; Desktop, Mobile 390px và reload PASS cho trạng thái chưa đăng nhập/đang xác minh. Không tràn ngang.
- PostgreSQL: bản Backend kiểm thử khởi động ở 8085, schema validate; `/api/san` HTTP 200, 17 sân. Backend IntelliJ hiện có ở 8084 cũng trả HTTP 200. Không dừng Backend IntelliJ của người dùng; cần restart Run Configuration để nạp code/cấu hình mới.
- MinIO public: đọc thử một ảnh sân hiện có trên server thật HTTP 200.
- MinIO upload thật: FAILED, PUT trả `SignatureDoesNotMatch` HTTP 403. Không có ảnh thử được upload thành công, không thay ảnh sân hoặc dữ liệu cũ. Chưa thể xác nhận toàn luồng upload → lưu PostgreSQL → Admin/Customer reload.
- VNPay Sandbox ngân hàng thật: NOT TESTED. Có credential cấu hình nhưng chưa thực hiện giao dịch qua ngân hàng/OTP. Không đồng nhất callback giả lập có chữ ký với giao dịch thật.
- IPN từ VNPay qua Internet: NOT TESTED, chưa có URL công khai. Handler, các mã phản hồi và cập nhật đơn đã được kiểm thử local/H2.

## MinIO: nguyên nhân và thay đổi

Endpoint thực tế trong IntelliJ: `http://103.167.88.223:9000`, bucket `nhom2`; không dùng Console 9001. Credential có giá trị và không có khoảng trắng đầu/cuối; không in giá trị. Shell không có cùng credential như IntelliJ, vì vậy kiểm thử storage đọc đúng cấu hình IntelliJ.

Trước sửa, upload gọi `bucketExists`, có thể kéo theo signed GET `?location`, rồi tạo bucket nếu thiếu và lấy presigned URL. Với bucket public đã được cung cấp, các bước đó không cần thiết.

Sau sửa:

- MinioClient có region từ `MINIO_REGION`, mặc định `us-east-1`, tránh tự truy vấn region. Kiểm tra endpoint không chứa đường dẫn bucket và không dùng cổng Console 9001.
- Upload thẳng vào bucket đã cấu hình, không tự tạo bucket.
- Ảnh được kiểm tra MIME/header và giới hạn 10 MB; tên UUID, extension theo MIME. Lưu object key `san/<uuid>.<extension>`.
- API upload trả `objectKey` và public URL, không cần tạo presigned URL sau PUT.
- Admin, CourtCard/trang chủ/danh sách/yêu thích và chi tiết dùng cùng helper URL ảnh. Chuẩn hóa object key có/không có tiền tố bucket; không ghép `nhom2` hai lần. URL sở hữu cũ bỏ query chữ ký hết hạn; dữ liệu không hợp lệ và ảnh lỗi dùng fallback.
- Upload lỗi không cập nhật form object key; ảnh cũ trong form và PostgreSQL không bị ghi đè. Upload thành công rồi nhấn Lưu mới cập nhật sân trong PostgreSQL.

Đã thử PUT ảnh PNG riêng bằng SDK trên server thật với credential IntelliJ và region `us-east-1`: vẫn 403 `SignatureDoesNotMatch`. Server không gửi gợi ý region trong header. Thời gian server khớp thời gian kiểm thử, không có bằng chứng lệch đồng hồ lớn. Do đó lỗi không chỉ nằm ở GET location. Chưa xác định dứt điểm giữa credential/region thực tế của server và canonical request/proxy; không tự kết luận credential đúng hoặc sai chỉ từ lời xác nhận trước đó. Cần giảng viên kiểm tra credential của đúng server/bucket và region. Không thay Access/Secret Key hoặc tắt ký request.

## VNPay: nguyên nhân và luồng mới

Trước sửa, `/return` trả văn bản 200/400, chưa redirect về React; `/ipn` chỉ nằm trong permitAll nhưng chưa có controller. Return URL IntelliJ là localhost, không phù hợp khi khách mở website bằng IP LAN trên thiết bị khác.

Luồng local mới:

1. Khách tạo đơn giữ chỗ 15 phút; tạo URL thanh toán giữ nguyên kiểm tra chủ đơn và snapshot giá.
2. VNPay đưa trình duyệt về Backend `/api/thanh-toan/vnpay/return`.
3. Backend kiểm tra tham số không lặp, chữ ký HMAC-SHA512, merchant/reference, số tiền và trạng thái.
4. Khi `VNPAY_RETURN_SETTLEMENT_ENABLED=true`, Return dùng chung bộ xử lý với IPN để cập nhật giao dịch an toàn. Không tin trạng thái do Frontend tự gửi.
5. Backend HTTP 303 redirect về React `/ket-qua-thanh-toan?ref=<reference>`; không chuyển Hash Secret hoặc JWT trong URL.
6. React gọi GET trạng thái có JWT, chỉ chủ đơn được xem. Hiển thị SUCCESS, FAILED, CANCELLED, VERIFYING hoặc REFUND_REVIEW; có nút lịch sử.
7. Khi VERIFYING, polling mỗi 3 giây, tối đa 20 lần; sau đó hướng người dùng xem lịch sử. Reload chỉ đọc lại trạng thái.
8. IPN bật sau này dùng cùng xử lý khóa đơn/transaction, không xác nhận lại giao dịch đã xử lý. Thanh toán đến muộn/hủy vẫn chỉ đánh dấu cần kiểm tra hoàn tiền, không khôi phục đơn hoặc tự hoàn tiền.

Return sai chữ ký trả HTTP 400, không cập nhật DB. Return có chữ ký hợp lệ nhưng số tiền sai không xác nhận thanh toán; trang React chỉ hiển thị trạng thái thật từ DB. Không dùng query trình duyệt để kết luận thành công.

### API

- POST `/api/thanh-toan/vnpay/create/{datSanId}`: khách hàng tạo URL cho đơn của mình.
- GET `/api/thanh-toan/vnpay/return`: callback trình duyệt, xác thực và redirect.
- GET `/api/thanh-toan/vnpay/ipn`: callback public xác thực bằng chữ ký, HTTP 200 JSON `RspCode`/`Message`.
- GET `/api/thanh-toan/vnpay/status/{reference}`: JWT KHACH_HANG, kiểm tra chủ đơn.

IPN: 00 xử lý/ghi nhận xong; 02 đã xử lý; 01 không tìm thấy giao dịch; 04 sai số tiền; 97 sai chữ ký/tham số. Lỗi hệ thống/transaction trả lỗi HTTP để gateway có thể gửi lại. Callback lặp, callback thất bại và callback đến muộn được kiểm thử. Refund flag không có nghĩa đã hoàn tiền.

Tham khảo protocol: https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html

## Cấu hình local đã cập nhật

Trong `.idea/workspace.xml` (Git ignored), giữ nguyên credential và thêm/cập nhật:

```properties
VNPAY_RETURN_URL=http://192.168.1.8:8084/api/thanh-toan/vnpay/return
VNPAY_FRONTEND_RESULT_URL=http://192.168.1.8:3004/ket-qua-thanh-toan
VNPAY_RETURN_SETTLEMENT_ENABLED=true
VNPAY_IPN_URL=
MINIO_REGION=us-east-1
```

Endpoint/bucket IntelliJ đã đúng. `.env.example` đồng bộ endpoint/bucket và thêm các biến không bí mật. Không copy credential xuống Frontend. Spring dev import `.env.local`, nhưng Environment Variables của Run Configuration có thể ghi đè file, cần chạy đúng cấu hình đã cập nhật.

### Khi deploy

- Cung cấp URL HTTPS thực tế của Backend/Frontend cho `VNPAY_RETURN_URL`, `VNPAY_FRONTEND_RESULT_URL`, `CORS_ORIGINS`, `VITE_API_BASE_URL`.
- `VNPAY_IPN_URL` để trống local; khi deploy điền URL Backend HTTPS công khai kết thúc bằng `/api/thanh-toan/vnpay/ipn`, đăng ký URL đó với VNPay. Biến này ghi cấu hình vận hành; không tự đăng ký với VNPay và không được gửi như tham số thanh toán.
- Không dùng localhost hoặc IP LAN làm IPN công khai. Kiểm tra VNPay thật gọi được handler rồi đặt `VNPAY_RETURN_SETTLEMENT_ENABLED=false` nếu muốn chỉ IPN cập nhật; Return vẫn xác thực và redirect, React chờ trạng thái.
- Cung cấp TmnCode/Hash Secret đúng môi trường qua secret environment, không commit.
- MinIO cần region chính xác và credential được giảng viên kiểm tra trên đúng server. HTTPS website cần public MinIO HTTPS/reverse proxy để tránh mixed content; cấu hình `VITE_MINIO_PUBLIC_URL` là base URL public không chứa credential.
- Hosting React cần rewrite các route SPA về index.html, bao gồm `/ket-qua-thanh-toan`.

## File sửa chính

Backend:
- src/main/java/com/example/becicd/config/MinIoConfig.java
- src/main/java/com/example/becicd/config/RequiredConfiguration.java
- src/main/java/com/example/becicd/config/VNPayConfig.java
- src/main/java/com/example/becicd/service/MinioService.java
- src/main/java/com/example/becicd/service/VNPayService.java
- src/main/java/com/example/becicd/controller/SanAdminController.java
- src/main/java/com/example/becicd/controller/VNPayController.java
- src/main/resources/application.yml
- .env.example; cấu hình IntelliJ local ignored.
- BookingIntegrationTest.java, SystemHttpIntegrationTest.java, MinioSafetyTest.java.

Frontend:
- fe/src/utils/fieldImage.js (mới)
- fe/src/pages/PaymentResultPage.jsx (mới)
- fe/src/App.jsx
- fe/src/components/CourtCard.jsx
- fe/src/components/FieldPresentation.jsx (giữ phần thay đổi đang có của người dùng)
- fe/src/pages/AdminPages.jsx
- fe/tests/render.test.cjs

Tài liệu LUONG_VNPAY_CHI_TIET.md được đánh dấu là mô tả luồng trước sửa, trỏ về báo cáo này.

## Chạy lại và kiểm tra thực tế

1. Restart Backend bằng Run Configuration IntelliJ đã cập nhật, port 8084, ddl-auto=validate. Không bật demo.seed.enabled.
2. Frontend chạy Vite 3004; truy cập bằng cùng host LAN đã cấu hình. Đăng nhập khách hàng, đặt sân VNPay, hoàn tất Sandbox thủ công và kiểm tra trở về React, trạng thái từ Backend và lịch sử. Không dùng ref `verification` như giao dịch thật.
3. Test giao dịch thành công, thất bại/hủy, reload và gửi callback lặp. Chỉ báo PASS giao dịch thật sau bước này.
4. Sau khi MinIO credential/region được xác minh: Admin sửa sân → upload ảnh PNG/JPG/WebP → Lưu → reload Admin/Customer/chi tiết/yêu thích. Kiểm tra file quá 10MB/sai loại và upload lỗi vẫn giữ ảnh cũ. Hiện nhóm kiểm thử toàn luồng này NOT TESTED do signed PUT 403.
5. Khi có IPN công khai, đăng ký với VNPay và thực hiện giao dịch thật để xác minh nhận IPN; bật chế độ IPN-only sau khi kiểm chứng.

Không commit/push, không thay Database schema hoặc dữ liệu thanh toán thật. Log, IDE settings và probe trong target đều bị Git ignore.

Xác minh cuối với JAR mới nhất: Backend 8085 khởi động thành công với PostgreSQL validate; /api/san HTTP 200 (17 sân), IPN thiếu chữ ký trả RspCode 97. Đã dừng đúng Backend kiểm thử 8085 sau kiểm tra; Backend IntelliJ 8084 được giữ nguyên. Vite Frontend 3004 đang chạy để xem giao diện mới.
