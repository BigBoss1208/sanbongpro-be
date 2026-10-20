# Admin upload/thay ảnh — 13/10/2026

## Kết quả

Chưa sửa dứt điểm upload MinIO thật: thử PUT ảnh PNG riêng bằng SDK và credential IntelliJ vẫn nhận SignatureDoesNotMatch HTTP 403 lúc 22:54:56 (Asia/Saigon). Không thay key, endpoint, bucket hoặc cách hiển thị ảnh trực tiếp đang hoạt động. Không thay VNPay trong yêu cầu này.

Endpoint đúng http://103.167.88.223:9000, bucket nhom2, region đang cấu hình us-east-1 nhưng chưa được server xác nhận. Credential có giá trị, không có khoảng trắng, không bọc dấu nháy hoặc placeholder. Lỗi tái hiện bên ngoài Controller, nên không có bằng chứng lỗi chữ ký do MultipartFile/React. Ảnh public hiển thị không chứng minh credential ghi dữ liệu hợp lệ.

## Giảng viên cần xác nhận

1. Region server/bucket chính xác (không đoán us-east-1).
2. Access Key thuộc tài khoản đang enabled và Secret Key tương ứng còn hiệu lực trên đúng server; có thay đổi/rotate gần đây hay không. Không gửi giá trị secret vào chat, cập nhật trực tiếp cấu hình local khi đã xác nhận.
3. Policy có PutObject cho nhom2/san/*; phân biệt AccessDenied với SignatureDoesNotMatch.
4. Endpoint 9000 có proxy/rewrite Host/path/query hay không; đối chiếu log PUT bị 403 tại thời điểm trên để xác định nguyên nhân ký không khớp.
5. Không yêu cầu đổi public policy để sửa chữ ký; không tắt xác thực.

## Sửa có thể kiểm thử

- ImageUploadField trước đây không có file picker khi đã có ảnh: phải Xóa ảnh trước khi chọn mới, làm mất ảnh cũ trong form nếu upload lỗi. Thêm thao tác Thay ảnh trong cùng control, giữ preview/object key cũ trong khi upload và chỉ đổi sau thành công. Không thiết kế lại trang.
- Disable xóa/chọn ảnh trong lúc upload; không đóng form Admin sân trong lúc upload/save, tránh callback cập nhật nhầm form mới.
- Reset file input sau khi chọn để thử lại cùng file sau lỗi.
- Backend trả 502 và mã STORAGE_UPLOAD_REJECTED khi MinIO trả lỗi xác thực; log chỉ error code/status, không dump request, credential hoặc chữ ký.
- Upload không cập nhật DB; PUT /api/admin/san/{id} khi Lưu mới cập nhật object key. Không xóa object ảnh cũ.

## Kiểm thử

FE build/lint và 23 test PASS. Test control có ảnh cũ vẫn cho chọn ảnh mới và khóa xóa trong lúc upload.

BE kiểm thử HTTP H2 mô phỏng MinIO 403: API trả 502, hinhAnh cũ không đổi. Upload thành công giả lập vẫn giữ DB cũ đến khi Lưu, sau PUT DB và danh sách public trả key mới. Đây là test local, không phải upload thật MinIO PASS. Kết quả build/test Backend cuối được ghi ở cuối báo cáo.

MinIO upload thật FAILED 403. Thay ảnh trên PostgreSQL thật/kiểm tra ảnh mới thật: NOT TESTED vì chưa có PUT thành công, không tự ghi ảnh giả vào Database thật.

## File sửa trong yêu cầu này

- fe/src/components/ui.jsx
- fe/src/pages/AdminPages.jsx
- fe/tests/render.test.cjs
- src/main/java/com/example/becicd/controller/SanAdminController.java
- src/test/java/com/example/becicd/SystemHttpIntegrationTest.java

Các chỉnh sửa cấu hình MinIO/object key/public URL từ yêu cầu trước được giữ nguyên, không đổi key hoặc thêm bucket vào endpoint.

## Git

Đã fetch origin thành công cho cả Backend/Frontend, không thay remote. Backend HEAD 7 ahead/3 behind origin/main; Frontend 4 ahead/0 behind origin/main trước commit mới. Không merge hoặc xử lý conflict tự động.

Rà soát file chuẩn bị commit: không phát hiện exact match các secret thật đang cấu hình. .env.local, .idea, target, node_modules, dist, log và backup bị ignore; chỉ .env.example template được theo dõi. Không đưa root package-lock.json chưa rõ nguồn thay đổi vào commit source.

Lịch sử Backend d88e953 vẫn reachable từ origin/main, chứa literal database password, MinIO Access/Secret Key và VNPay Hash Secret. Theo yêu cầu trước phải dừng push khi phát hiện secret trong lịch sử. Không rotate key, rewrite lịch sử hoặc force push. Commit source hợp lệ chỉ ở local branch hiện tại; push main bị chặn đến khi chủ repository xử lý credential/lịch sử và cho phép tiếp tục. Không báo push thành công.

Kết quả cuối: Backend Maven package BUILD SUCCESS, 47/47 test PASS; Frontend build/lint PASS, 23/23 test PASS. Chỉ là kiểm thử local/mock cho nhánh thành công, MinIO thật vẫn FAILED 403.
