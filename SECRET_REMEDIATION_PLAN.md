# Xử lý secret Backend — 14/10/2026

Trạng thái: CHƯA HOÀN TẤT. Source hiện tại dùng environment variables; lịch sử vẫn có secret. PostgreSQL password và VNPay Hash Secret lịch sử trùng cấu hình IntelliJ hiện tại. MinIO key lịch sử khác cấu hình hiện tại, nhưng chưa xác nhận đã bị thu hồi trên server.

Không ghi giá trị secret trong tài liệu này. Không thay credential, Database, nhánh remote hoặc lịch sử Git. Không force push.

## Việc cần chủ dịch vụ và Owner GitLab thực hiện

1. Chủ PostgreSQL và VNPay thu hồi/rotate credential đã lộ, đồng bộ credential mới trong cấu hình local/deploy qua kênh bí mật. Chủ MinIO xác nhận các key lịch sử đã bị thu hồi hoặc thu hồi chúng. Không gửi key vào chat/Git.
2. Chủ GitLab chọn Redact text trong Settings > Repository > Repository maintenance để thay secret bằng ***REMOVED***, giữ nội dung source còn lại. Cần Owner; credential Git hiện tại bị API từ chối HTTP 401 nên agent chưa xác minh/thi hành được quyền này.
3. Đây vẫn là thao tác rewrite lịch sử phía server, thay commit SHA và không thể đảo ngược; cần thống nhất với người cùng làm trước khi thực hiện. Không áp dụng git filter-repo/force push từ local, không xóa nguyên application.yml bằng Remove blobs khi chỉ cần che secret.
4. Theo quy trình GitLab, hoàn tất housekeeping/prune để xử lý các đối tượng/cache. Nếu có fork, cần phối hợp chủ fork/GitLab Support.
5. Sau khi hoàn tất, clone mới; giữ riêng các thay đổi local chưa commit (root package-lock.json). Không push lại branch/tags cũ vì có thể đưa secret trở lại.
6. Rà soát lịch sử mới và xác nhận credential cũ không còn hiệu lực, rồi tạo MR Backend vào main và chạy lại CI. Các commit SHA cũ trong báo cáo chỉ dùng đối chiếu trước remediation.

Tài liệu chính thức: https://docs.gitlab.com/user/project/repository/repository_size/#remove-data-from-a-repository
Việc xóa lịch sử không thay thế thu hồi secret: https://docs.gitlab.com/user/application_security/secret_detection/pipeline/

## Danh sách phiên bản file có literal secret

Đây là inventory để Owner đối chiếu, không phải yêu cầu xóa toàn bộ file. Không chứa giá trị secret.

- Blob `14b8f1adf43445861c01c683386dd8b03be8d5b0` — `src/main/resources/application.yml`; loại: access-key, hash-secret, password, secret-key; commit ví dụ `d88e9535b059`.
- Blob `746323422f2af6ec9fd924c32753094d1db8e151` — `src/main/resources/application.yml`; loại: access-key, hash-secret, password, secret-key; commit ví dụ `cc47f4fd81e0`.
- Blob `a87930c3aa3fa12514cba624245d9cd3790006c0` — `src/main/resources/application.yml`; loại: access-key, hash-secret, password, secret-key; commit ví dụ `4c564d6e53d7`.
- Blob `9e1a2b855920ae30db43c530789092d4da20e54d` — `src/main/resources/application.yml`; loại: access-key, password, secret-key; commit ví dụ `1291598d80aa`.
- Blob `64b80d2b548ed4e2233f0c9d56b065dd3b28f3b1` — `src/main/resources/application.yml`; loại: password; commit ví dụ `7b0eb48e5050`.

## Điều kiện để tiếp tục

Cần người có quyền trên PostgreSQL/VNPay/MinIO xác nhận thu hồi credential cũ và Owner GitLab thực hiện/cho phép phương án redaction. Hiện chưa có quyền server dịch vụ, API GitLab không chấp nhận credential sẵn có. Không thể báo đã xử lý hoàn tất hoặc Backend main đã an toàn.
