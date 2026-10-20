# Chạy Backend SÂN BÓNG PRO

Source đã điền các địa chỉ không bí mật: PostgreSQL 103.167.88.223:5432/ra99_n2; MinIO http://103.167.88.223:9000, bucket nhom2; Backend 8084; Frontend local 3004. Environment Variables vẫn ghi đè các giá trị mặc định.

## Anh chạy trên máy cá nhân

1. Dùng Backend nhánh update-backend-0926. Các file thay đổi mới chỉ có local cho đến khi push được xác minh.
2. Nếu chưa có .env.local, copy .env.example thành .env.local. KHÔNG ghi đè .env.local đang có.
3. Chạy `powershell -ExecutionPolicy Bypass -File scripts/Initialize-Local.ps1` để tạo JWT secret ngẫu nhiên trong file ignored; script giữ secret đã có. Không đưa file đó lên Git.
4. Điền DB_USERNAME, DB_PASSWORD, MINIO_ACCESS_KEY, MINIO_SECRET_KEY, VNPAY_TMN_CODE, VNPAY_HASH_SECRET vào .env.local bằng credential được chủ dịch vụ cấp. Không dùng lại credential đã lộ; không gửi credential trong chat hoặc commit.
5. Trong IntelliJ, đặt working directory là thư mục Backend, active profile dev. Spring đọc .env.local qua application-dev.yml. Nếu Run Configuration đã có biến môi trường thì chúng ghi đè file; không dùng DB_URL MySQL của dự án khác.
6. JWT_EXPIRATION_MS=86400000; JWT_SECRET tối thiểu 32 UTF-8 bytes. Không tắt validation JWT hoặc ddl-auto=validate; không bật demo.seed.enabled.
7. Kiểm tra TCP `Test-NetConnection 103.167.88.223 -Port 5432`, rồi chạy Backend và gọi http://localhost:8084/api/san. Timeout TCP cần chủ server/mạng xử lý, không sửa password để chữa lỗi mạng.

## Chạy trên server hoặc qua LAN

Đặt CORS_ORIGINS đúng origin Frontend, VNPAY_RETURN_URL trỏ tới Backend /api/thanh-toan/vnpay/return, VNPAY_FRONTEND_RESULT_URL trỏ tới Frontend /ket-qua-thanh-toan. localhost chỉ phù hợp khi trình duyệt và các dịch vụ cùng máy. Frontend VITE_API_BASE_URL cần trỏ đúng Backend hoặc dùng reverse proxy.

VNPAY_IPN_URL để trống khi chưa có Backend HTTPS công khai đăng ký VNPay. Không điền URL giả/IP LAN. MINIO_REGION mặc định us-east-1 chưa được chủ server xác nhận. Lỗi MinIO 403 còn cần giảng viên xác minh; không đổi key tự động.

## Git và giới hạn

Không hard-code secret trong application.yml. .env.example là mẫu trống credential, không phải cấu hình đủ để chạy khi chưa được cấp tài khoản. .env.local và .idea bị ignore. Lịch sử Backend còn secret và phải được chủ dịch vụ thu hồi cùng Owner GitLab xử lý trước khi tiếp tục publish theo yêu cầu bảo mật đã đặt.
