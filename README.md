# SÂN BÓNG PRO — Backend local

Backend mặc định chạy port **8084**. Chạy mọi lệnh từ thư mục repository Backend.

## JWT và cấu hình local

Spring không tự đọc `.env`. Profile `dev` hiện nhập `.env.local` theo định dạng Java properties; file này bị Git bỏ qua. Không chép secret vào `application.yml`, README hoặc GitLab.

```powershell
.\scripts\Initialize-Local.ps1
$env:SPRING_PROFILES_ACTIVE = 'dev'
mvn spring-boot:run
```

Script tạo JWT secret 48 byte ngẫu nhiên bằng bộ sinh mật mã, khi key chưa có hoặc đang rỗng trong `.env.local`. Secret được giữ qua các lần khởi động. Khi chạy trong IntelliJ, chọn profile `dev` và đặt Working directory bằng thư mục Backend. `JWT_SECRET` trong biến môi trường được ưu tiên hơn file local. Có thể tạo biến môi trường mà không in secret:

```powershell
$jwtBytes = New-Object byte[] 48
$jwtRandom = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $jwtRandom.GetBytes($jwtBytes); $env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes) }
finally { $jwtRandom.Dispose() }
```

Biến `$env:` chỉ áp dụng cho terminal hiện tại và tiến trình con; một IDE đang mở không tự nhận chúng. Cấu hình DB, MinIO, VNPay và CORS vẫn bắt buộc: dùng tên biến trong `.env.example`, hoặc thêm vào `.env.local` theo dạng `NAME=value`. File mẫu không chứa thông tin đăng nhập. Thiếu JWT hoặc key dưới 32 byte vẫn làm startup thất bại có thông báo rõ ràng; không có secret mặc định cho production.

## Khởi động kiểm tra cô lập, không cần PostgreSQL/MinIO

```powershell
.\scripts\Initialize-Local.ps1 -Isolated
mvn clean verify
java -jar target/be-cicd-0.0.1-SNAPSHOT.jar --spring.profiles.active=smoke
Invoke-RestMethod http://127.0.0.1:8084/api/health
Invoke-RestMethod http://127.0.0.1:8084/api/san
```

Profile `smoke` chỉ dùng để kiểm tra local: bind loopback, H2 RAM và `create-drop`; dữ liệu mất khi dừng. `.env.smoke` chứa cấu hình kiểm thử do script sinh và bị Git bỏ qua. Không bật profile này trên production. MinIO/VNPay là cấu hình chưa kết nối dịch vụ, không dùng để upload/thanh toán thật. Script không ghi đè file local đã có. Muốn dùng hệ thống thực, chạy profile `dev` với các thông số dịch vụ local hợp lệ. Logic VNPay không thay đổi.

## IntelliJ local và chẩn đoán JWT

Máy local hiện có Run Configuration `BeCicdApplication Local Smoke` (file trong `.idea/runConfigurations`, bị Git bỏ qua): profile `smoke`, Working directory `$PROJECT_DIR$`, port 8084, JWT expiration 86400000 ms. Secret được đọc từ `.env.local`, không cần sao chép vào Run Configuration. Nếu IntelliJ đang giữ cấu hình cũ, chọn cấu hình Local Smoke đã lưu. Với dịch vụ local thật, chọn profile `dev` và cung cấp cấu hình DB/MinIO/VNPay như phần trên.

Lỗi JWT hiện chỉ rõ nguyên nhân riêng: thiếu/rỗng key, key dưới 32 byte UTF-8 hoặc expiration không dương. Có key trong `.env.local` nhưng không bật profile dev/smoke vẫn là key chưa được nạp. Không thay key hợp lệ chỉ để xử lý lỗi profile.
