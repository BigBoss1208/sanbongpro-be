# SÂN BÓNG PRO — giai đoạn 1 cloud

## Phạm vi và lịch sử

Đây là bản sao source hiện tại, gồm các thay đổi local hợp lệ (kể cả Gmail), chuẩn bị cho repo cá nhân. Không clone/push lịch sử nhóm vì lịch sử đó có credential. Repo nhóm, nhánh, origin và GitHub remote cũ được giữ nguyên. Bản mới bắt đầu main không có commit cũ; chưa commit/push/deploy. Không dùng `push --mirror` hoặc thêm lịch sử nhóm trở lại bản này. Cần đổi/revoke credential từng lộ trong lịch sử; làm sạch bản mới không thu hồi được secret đã lộ ở repo cũ. Các tài liệu VPS cũ chỉ mang tính lịch sử, không phải hướng dẫn cloud.

## Kết nối GitLab cá nhân sau khi được xác nhận

Tạo hai project PRIVATE trống `san-bong-pro-fe`, `san-bong-pro-be`, không khởi tạo README. URL cá nhân chưa được cung cấp nên không tự đặt remote. Trong đúng từng bản sao, review `git status`, security scan và diff trước khi add/commit. Sau đó mới thêm origin URL chính xác của repo cá nhân và push main khi được cho phép. Không thực hiện các lệnh này trong repo nhóm. Không nhập password/token vào URL. Giữ PRIVATE cho tới khi hoàn tất review. GitLab CI mới chỉ verify, không còn lệnh deploy VPS, docker rm/prune hoặc push image. Runner cá nhân cần được cấp/quota; không dùng runner nhóm khi chưa được phép.

## Frontend Cloudflare Pages

Kết nối repo FE cá nhân khi được cho phép. Framework React/Vite; root repo, Node 22, build `npm run build`, output `dist`. Đã có public/_redirects cho SPA deep links như /lich-su-dat-san và /ket-qua-thanh-toan. VITE_API_BASE_URL = HTTPS origin Render thật, không thêm /api; VITE_MINIO_PUBLIC_URL = HTTPS bucket URL thật kết thúc /nhom2. Không có secret nào được đặt trong VITE_*; build lại khi đổi biến. UI, API và redirect logic giữ nguyên. Cấu hình production/preview Pages riêng; CORS chỉ mở đúng origin đã duyệt, không wildcard toàn bộ previews.

## Backend Render + Neon

Dockerfile Java 11 hiện tại; Spring lắng nghe 0.0.0.0 và `${PORT:8084}`, dùng PORT Render cung cấp. render.yaml là template, autoDeployTrigger off; tạo service/Blueprint vẫn có thể bắt đầu deploy lần đầu nên CHƯA tạo ở giai đoạn này. Template plan starter trả phí để giữ Gmail SMTP, không tự đăng ký/mua. Health /api/health có kiểm tra DB và được permit public. Biến bắt buộc lấy từ .env.example; không dùng file .env của máy dev, không bật profile smoke/dev trên cloud. Không ghi credential vào Docker build args/layers.

DB_URL dạng `jdbc:postgresql://<Neon-host>:5432/<database>?sslmode=verify-full`; DB_USERNAME/DB_PASSWORD riêng. Không copy postgres:// URI nguyên xi vào JDBC và không để password trong URL. Profile cloud bắt buộc SSL verify-full; giữ hostname Neon thật để kiểm tra chứng chỉ. Hikari tối đa 5 kết nối, minimum idle 0. Chưa tạo Neon hoặc kiểm tra connection thật.

Giữ ddl-auto=validate, không create/update. Neon trống sẽ KHÔNG khởi động được cho tới khi có schema đầy đủ. Các SQL hiện tại là incremental migrations, không phải schema baseline đầy đủ. Việc xuất schema hoặc chuyển dữ liệu cần phê duyệt riêng; giai đoạn này không kết nối/sửa DB VPS thầy, không sao chép dữ liệu khách hàng, không tự chạy migration hay seed admin.

## MinIO / VNPay / Gmail

- MinIO: giữ client và bucket nhom2, key đưa vào Render Environment. Endpoint hiện có chỉ HTTP; Pages HTTPS có thể chặn ảnh HTTP. Cần thầy cung cấp public HTTPS MinIO endpoint/chứng chỉ hợp lệ. VITE_MINIO_PUBLIC_URL phải dùng HTTPS đó. Ảnh trong DB là absolute HTTP URL cũng cần kiểm tra và kế hoạch chuyển riêng; chưa sửa dữ liệu hay thêm proxy. Không tự đoán endpoint HTTPS hoặc đổi key/region. Upload thật từ Render chưa test.
- VNPay: VNPAY_RETURN_URL = HTTPS origin Render thật + /api/thanh-toan/vnpay/return; VNPAY_FRONTEND_RESULT_URL = HTTPS origin Pages thật + /ket-qua-thanh-toan. VNPAY_IPN_URL chỉ điền endpoint Render thật /api/thanh-toan/vnpay/ipn và đăng ký với VNPay khi deploy. Giữ return settlement true khi chưa có IPN; logic xác thực/chống callback trùng giữ nguyên. Không dùng URL giả hoặc trả tiền thật giai đoạn này.
- Gmail: giữ Spring Mail smtp.gmail.com:587 + STARTTLS và App Password trong GMAIL_APP_PASSWORD. Render Free chặn outbound SMTP 25/465/587; để giữ tích hợp hiện tại cần gói cho phép SMTP. GMAIL_ENABLED false mặc định, chỉ bật khi cấu hình thật và đã được duyệt gửi thử. Chưa gửi email giai đoạn này. Chống gửi trùng hiện tại dựa vào chuyển trạng thái thanh toán; chưa có hàng đợi/retry bền vững, không hứa exactly-once qua mọi sự cố.
- Không có dependency Redis cần triển khai.

## Chưa kiểm thử môi trường thật

Chưa tạo account/resource, chưa cấp URL cloud, chưa xác minh Neon schema/SSL/login, Render startup/SMTP/MinIO outbound, Pages HTTPS/mixed content, CORS production, VNPay Return/IPN trên Internet hay Gmail receipt cloud. Không có Docker local để build image. Build/tests local dùng dữ liệu H2 cô lập, không chứng minh dịch vụ cloud đã chạy.

Nguồn chính thức:
- https://developers.cloudflare.com/pages/configuration/build-configuration/
- https://developers.cloudflare.com/pages/configuration/serving-pages/
- https://render.com/docs/docker
- https://render.com/docs/blueprint-spec
- https://render.com/docs/free
- https://neon.com/docs/guides/java
