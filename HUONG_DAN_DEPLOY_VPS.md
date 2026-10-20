# Triển khai GitLab nhóm lên VPS — kiểm tra ngày 20/10/2026

Chưa push, chưa chạy deploy hoặc thao tác container trên VPS. Giữ nguyên hai origin của nhóm. Các lệnh dưới đây dành cho người quản lý VPS thực hiện sau khi xác nhận; không chạy nguyên danh sách nếu chưa kiểm tra quyền sở hữu tài nguyên.

## 1. Nguyên nhân đã có log

- BE #2945072172: deploy thất bại với `docker: open : no such file or directory.` APP_ENV_FILE rỗng tại lệnh `docker run --env-file`. Registry login và pull image đã thành công.
- FE #2945114044: deploy thất bại với `docker: no name set for network.` DOCKER_NETWORK rỗng. Log không cho biết API_UPSTREAM đã có giá trị hay chưa.
- Chuỗi `test ... && test ...` cũ không ngăn job chạy tiếp khi kiểm tra không đạt. Cả hai job đã xóa container tên đích trước khi lệnh run thất bại.
- Bản CI local đã thêm kiểm tra dừng rõ ràng trước bước xóa, kiểm tra network tồn tại, kiểm tra nginx trước khi thay FE; bỏ Docker prune toàn máy. Chưa có lần deploy xác nhận bản sửa.

## 2. Kiểm tra VPS chỉ đọc trước khi cho phép deploy

Người quản lý xác nhận chính xác host, hai runner `sports-be` / `sports-fe` và Docker daemon mà chúng sử dụng. Cùng một network name trên hai Docker daemon khác nhau không kết nối được hai container.

```sh
docker ps -a --format 'table {{.Names}}\t{{.Image}}\t{{.Ports}}\t{{.Status}}'
docker network ls
ss -ltn '( sport = :3004 or sport = :8084 )'
# Sau khi xác định tên network thực tế:
docker network inspect "$DOCKER_NETWORK" --format '{{.Name}} {{range .Containers}}{{.Name}} {{end}}'
```

Xác nhận `sports-be` và `fe-cicd` thực sự thuộc dự án này; nếu thuộc nhóm khác thì DỪNG. Hai pipeline hiện dùng các tên cố định đó. Xác nhận cổng host 3004 và 8084 không thuộc dịch vụ khác; nếu đang được chính container dự án chiếm thì ghi nhận đó là tài nguyên sẽ thay thế. Không xóa container, network, volume hay dùng prune. Nếu cần network mới, xin phép quản lý trước khi tạo.

Trước deploy lưu thông tin image SHA hiện tại và phương án rollback riêng tư. Không xuất `docker inspect` đầy đủ vào chat/log vì có thể chứa environment secrets. Kiểm tra backup DB theo quy trình của thầy; deploy này không được tự chạy migration.

## 3. GitLab CI/CD Variables

Settings → CI/CD → Variables, Environment scope `staging` hoặc `*` phù hợp. Nếu Protected bật, nhánh main phải protected. Không đặt các giá trị này trong workflow/job log; không dùng `set -x`, `printenv` hoặc `cat` file bí mật.

| Repo | Key | Type / giá trị |
|---|---|---|
| BE + FE | DOCKER_NETWORK | Variable, tên network có thật, giống nhau và cùng daemon |
| FE | API_UPSTREAM | Variable, `http://sports-be:8084`, không localhost, không thêm `/api` |
| BE | APP_ENV_FILE | **File**, nội dung dạng `KEY=value`, không phải YAML và không nhập đường dẫn máy Windows |

Giữ VITE_API_BASE_URL rỗng để trình duyệt gọi `/api` qua nginx FE. Không bao giờ đưa credential vào biến VITE vì nó được đóng gói công khai trong JavaScript.

### Lưu ý quan trọng với application.yml hiện tại

File đang dùng literal cho datasource, JWT, MinIO, VNPay và CORS; Gmail đã dùng placeholders. Vì vậy file có các tên DB_URL/JWT_SECRET/VNPAY_* theo `.env.example` không tự động ghi đè các literal đó. Trước phát hành cần thống nhất một trong hai cách:

1. Thay các literal bằng placeholders tương ứng rồi kiểm thử lại, giữ secret trong môi trường riêng; không đẩy literal lên Git; hoặc
2. Với source giữ nguyên, dùng đúng tên Spring properties trong APP_ENV_FILE theo bảng dưới. Cách này ghi đè cấu hình lúc chạy nhưng KHÔNG loại secret có sẵn khỏi source/JAR; vẫn phải xử lý riêng trước phát hành.

| Property | Environment override cho source hiện tại |
|---|---|
| spring.datasource.url / username / password | SPRING_DATASOURCE_URL / SPRING_DATASOURCE_USERNAME / SPRING_DATASOURCE_PASSWORD |
| app.jwtSecret / app.jwtExpirationMs | APP_JWTSECRET / APP_JWTEXPIRATIONMS |
| app.cors-origins | APP_CORSORIGINS |
| minio.endpoint / access-key / secret-key / bucket / region | MINIO_ENDPOINT / MINIO_ACCESSKEY / MINIO_SECRETKEY / MINIO_BUCKET / MINIO_REGION |
| vnpay.tmn-code / hash-secret | VNPAY_TMNCODE / VNPAY_HASHSECRET |
| vnpay.return-url / ipn-url / frontend-result-url | VNPAY_RETURNURL / VNPAY_IPNURL / VNPAY_FRONTENDRESULTURL |
| vnpay.return-settlement-enabled | VNPAY_RETURNSETTLEMENTENABLED |

Đặt PORT=8084 và SPRING_JPA_HIBERNATE_DDL_AUTO=validate. JWT phải đủ ít nhất 32 UTF-8 bytes; expiration dương. Không cung cấp secret thật trong tài liệu này. Gmail sử dụng GMAIL_ENABLED/GMAIL_USERNAME/GMAIL_APP_PASSWORD như `.env.example` và App Password riêng.

CORS là origin FE thực tế. Return URL là địa chỉ Backend truy cập được từ trình duyệt + `/api/thanh-toan/vnpay/return`; frontend-result-url là địa chỉ FE thực tế + `/ket-qua-thanh-toan`. IPN chỉ điền URL Backend công khai thật `/api/thanh-toan/vnpay/ipn` đã đăng ký VNPay; không dùng URL giả. Có thể giữ return settlement bật khi chưa nhận IPN. Không dùng localhost của VPS làm địa chỉ trình duyệt người dùng.

## 4. Kết nối dịch vụ

Ngày 20/10/2026 từ máy phát triển: 103.167.88.223:5432 TCP reachable; 103.167.88.223:9000 TCP reachable, `/minio/health/live` HTTP 200. Đây chưa phải kiểm tra từ Docker network VPS, chưa xác minh DB login hoặc quyền upload MinIO. Không suy ra máy đó chính là VPS deploy.

Nhờ quản lý kiểm tra từ đúng môi trường chạy BE: DNS/route PostgreSQL, login đúng DB `ra99_n2`, schema `public`, quyền truy cập MinIO bucket `nhom2`; không đổi key, region hoặc dữ liệu để thử. Giữ ddl-auto=validate; nếu schema thiếu thì dừng, không tự migration. Không có dependency/cấu hình Redis trong source đã kiểm tra nên không cần dựng Redis cho deploy này.

## 5. Trình tự sau khi có xác nhận

1. Review chỉ các thay đổi được duyệt, loại credential khỏi source/image trước phát hành; không stage toàn bộ worktree vì còn thay đổi Gmail chưa push.
2. Sau khi được phép push, đưa CI đã sửa vào nhánh chạy pipeline bằng quy trình nhóm. CI ở local chưa có trên remote; retry job cũ vẫn dùng config của commit cũ.
3. Chạy BE build/test/package, sau đó mới bấm deploy manual khi đã được phép thay container. Theo dõi Spring startup/validate và API `/api/health`, `/api/san`.
4. BE sẵn sàng và DNS sports-be phân giải trong network rồi mới chạy FE deploy manual. Nginx preflight kiểm tra cấu hình/DNS, không chứng minh API hay DB đã sẵn sàng.
5. Kiểm tra FE cổng 3004, BE 8084 từ VPS và trình duyệt ngoài VPS; firewall do quản lý xác nhận, không tự mở cổng.

Không có Compose trong source; CI deploy trực tiếp bằng docker run. Không bổ sung một stack Compose song song gây trùng tên/cổng.

## 6. Kiểm tra sau deploy (chưa thực hiện)

- Chỉ đọc trước: `/api/health` trả 200, `/api/san` trả danh sách thật; FE tải ảnh trực tiếp từ MinIO; đăng nhập bằng tài khoản test được cho phép, kiểm tra phân quyền và lịch sử của chính người dùng.
- Đặt sân/VNPay/Gmail tạo dữ liệu hoặc gửi thư: chỉ thử khi được duyệt tài khoản, slot và sandbox. Kiểm tra PENDING → thanh toán đã xác thực → trạng thái Backend, Return redirect về FE, chữ ký sai/số tiền sai bị từ chối; callback trùng không xác nhận hoặc gửi email lần hai. Không phát lại giao dịch thật để thử.
- Kiểm tra Gmail tới hộp thư test; lỗi SMTP không làm sai trạng thái thanh toán. IPN end-to-end chỉ xác minh khi VNPay gọi được URL công khai đã đăng ký.
- Nếu lỗi startup, health, schema hoặc external services: dừng triển khai tiếp, không thay dữ liệu hoặc cấu hình nhóm khác. Container Running không đồng nghĩa Backend đã sẵn sàng.

Các kiểm tra build trước đây: FE lint/build và 23 tests đạt; BE build và 51 tests đạt. Chưa chạy lại trong phiên hướng dẫn ngày 20/10. Local FE/BE hiện không lắng nghe 3004/8084, máy không có Docker CLI. Chưa xác nhận port ownership, dịch vụ từ VPS, pipeline mới hoặc luồng nghiệp vụ sau deploy.
