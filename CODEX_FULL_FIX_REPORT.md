# SÂN BÓNG PRO — Báo cáo sửa lỗi và giao diện, cập nhật 13/10/2026

## Kết quả đợt trước (12/10/2026) và phạm vi bằng chứng

Đã chỉnh sửa code Frontend/Backend, bổ sung kiểm thử, chuẩn bị migration thủ công và nâng cấp trang chủ. Frontend build thành công, lint **0 lỗi/0 cảnh báo**, **14 test Frontend PASS**; Backend biên dịch và **26 test PASS, 0 fail/error/skipped**. Có kiểm thử HTTP qua Spring Security/MockMvc và JPA trên H2 thật trong bộ nhớ, không phải chỉ mock controller.

Không kết nối/ghi PostgreSQL production, không gọi cổng VNPay thật/sandbox qua mạng, không upload lên MinIO thật, không áp dụng migration và không triển khai. Không thay đổi secret thật hoặc các thay đổi có sẵn của người dùng (`package-lock.json` repository cha, trạng thái repository `fe`, thay đổi header từ lần trước). Commit/push được thực hiện trong đợt tiếp tục bên dưới. Dependencies nghiệp vụ không được thêm; tests dùng thư viện hiện có. Maven cần tải provider kiểm thử còn thiếu từ Maven Central; cache nằm trong `.m2/repository` của workspace, đã ignore.

Quyết định đã nhận từ người dùng: nhân viên chỉ được xem/xử lý cụm được phân công; chưa phân công thì không có quyền xử lý sân. Admin có quyền toàn hệ thống. Câu trả lời “30ph” được áp dụng theo ví dụ đã hỏi: check-in từ **30 phút trước giờ bắt đầu đến giờ kết thúc**. Không thêm quy trình đổi lịch, hoàn tiền tự động, sản phẩm demo hoặc danh mục loại sân độc lập.

`FIXED & TESTED` bên dưới chỉ xác nhận phạm vi test được ghi ở từng hàng, **không có nghĩa đã kiểm thử production hoặc E2E browser toàn bộ chức năng**. `FIXED — NOT RUNTIME VERIFIED` có code sửa nhưng chưa thử tương tác tương ứng với dịch vụ/môi trường thật. Những vấn đề còn lại được liệt kê cuối báo cáo.

## F01–F27

Đường dẫn viết ngắn trong bảng tính từ `D:/Năm 3/RaiWay99/QuanLiSanBong`; tiền tố Java chung là `src/main/java/com/example/becicd/`.

| Mã | Trạng thái trước | File đã sửa | Cách sửa | Test/bằng chứng | Trạng thái sau |
|---|---|---|---|---|---|
| F01 | Critical | application.yml, security/JwtTokenProvider, config/RequiredConfiguration, .env.example | Bỏ credentials thật/secret fallback khỏi cấu hình; env bắt buộc; JWT >=32 byte; startup kiểm cấu hình; ddl-auto mặc định validate | JWT weak-secret test; HTTP test khởi động bằng cấu hình cô lập | FIXED & TESTED |
| F02 | High | security/JwtAuthFilter | Kiểm khóa trước tạo Authentication; quyền lấy từ DB, không tin role claim; 403 khi khóa; không log token | Cùng JWT trước/sau khóa; HTTP token đã phát hành bị từ chối; role claim giả không nâng quyền | FIXED & TESTED |
| F03 | High | controller/NhanVienDatSanController, service/VNPayService | Gán soTien/tongTien từ snapshot giá; tạo giao dịch trong transaction | Online cash/VNPay và walkin; assertion số tiền; rollback JPA | FIXED & TESTED |
| F04 | High | service/BookingGuard; San/KhungGio/DatSanRepository; booking controllers | Khóa pessimistic sân trước kiểm/ghi; khóa khung giờ ngăn sửa lịch trong lúc tạo đơn; overlap theo thời gian, không chỉ ID | Hai thread đặt các ID khung giờ khác nhưng giao nhau: đúng một thành công; giờ liền kề không giao | FIXED & TESTED |
| F05 | High | BookingGuard, DatSanController, NhanVienDatSanController, GuestPages | Chặn ngày/giờ đã bắt đầu; validate sân/slot/method; FE ngày tối thiểu theo VN | H2 test quá khứ, null method guard source; HTTP booking hợp lệ | FIXED & TESTED |
| F06 | High | SanPublicController, SanAdminController, SanImageController, BookingGuard; adminApi | Công khai chỉ sân hoạt động; không đặt mới sân ngừng; admin GET riêng vẫn thấy sân ngừng; public image không lộ sân ẩn | API public/admin listing, booking sân ngừng, image 404 | FIXED & TESTED |
| F07 | High | BookingGuard, public/matrix/booking controllers | Chung hàm occupies cho giữ chỗ hết hạn; lịch xét overlap; slot quá giờ/ngừng đánh dấu không thể đặt mới | Expired hold nhường chỗ cho booking mới; callback muộn không hồi sinh đơn | FIXED & TESTED |
| F08 | High | DatSanController, NhanVienDatSanController, ThanhToanController | Hủy so ngày+giờ, chặn đơn check-in/hoàn thành; hủy/từ chối cập nhật payment trong transaction; pending → thất bại, paid → cờ cần xử lý; không tự refund | Hủy trước/sau trả tiền; confirm đơn hủy 409; callback sau hủy không hồi sinh | FIXED & TESTED |
| F09 | High | SanAdminController | PUT lưu/bỏ cumSanId, kiểm ID/cụm tồn tại; phản hồi DTO sau save | Unit đổi cụm/null/not-found, verify saved object | FIXED & TESTED |
| F10 | Đã xác nhận scope | BookingGuard, SanController, QuanLySanController, NhanVienDatSanController, ThanhToanController | Kiểm cụm cho đọc/duyệt/từ chối/tìm/check-in/walkin/thu tiền/hóa đơn nhân viên; admin global | HTTP search ngoài cụm trả [], confirm ngoài cụm 403; code guard ở mutations | FIXED & TESTED |
| F11 | High | entities CumSan/San/KhungGio/DatSan; SanAdmin/KhungGio/CumSanAdmin controllers; exception handler | Bỏ cascade ALL/REMOVE đến lịch sử; xóa có lịch sử/giá bị 409; cụm còn staff bị 409; FK conflict thông báo chung; chặn sửa giờ đã có lịch sử và POST có ID | HTTP xóa sân/giờ có booking 409, dữ liệu booking/payment còn; unit guard | FIXED & TESTED |
| F12 | Medium | GuestPages/ProfilePage, App, api/http | GET me trước sửa, khóa save khi load/error; cập nhật từ response, phát session event; avatar ghi rõ chỉ local | API me HTTP; source FE; chưa thao tác cập nhật hồ sơ qua browser với API thật | FIXED — NOT RUNTIME VERIFIED |
| F13 | Medium | dto/UserInput; Auth/AdminUser/NguoiDung controllers; NguoiDungRepository | Validate tên/email/username/SĐT/password theo schema/BCrypt; check email ignore-case tạo/sửa; giới hạn đổi giữa hai vai trò staff; null/password cũ rõ lỗi | Unit malformed data/auth/register/duplicate; HTTP đăng ký/login | FIXED & TESTED |
| F14 | Medium | PremiumHomePage, GuestPages, AdminPages, AdminOperationsPages | Bỏ catch rỗng ở page hoạt động; thêm load/error/retry; reset lỗi; retry bằng reloadKey; loading dashboard/profile; chống submit lặp form lưu; thông báo trạng thái | Browser home retry/login validation, FE client tests; còn thiếu kiểm thử DOM đủ mọi action quản trị | FIXED — LIMITED RUNTIME COVERAGE |
| F15 | Medium | ba config seeder/initializer | Chỉ profile dev + flag explicit true; mặc định tắt; thứ tự 1/2/3; seed loại sân phù hợp UI; không sửa dữ liệu cũ | Context test mặc định không seed demo; chưa chạy seed idempotency với DB dev được cấp | FIXED — NOT RUNTIME VERIFIED |
| F16 | High | SecurityConfig, VNPayController, VNPayService, VNPayConfig/application.yml | Chỉ khách hàng/chủ đơn tạo link; state/hạn hợp lệ; return public nhưng phải chữ ký/merchant/amount/status hợp lệ; URL env | Owner test; callback chữ ký/amount sai; HTTP return không Bearer tới controller trả 400 cho dữ liệu rỗng | FIXED & TESTED |
| F17 | Medium | PremiumHomePage, utils/courtSearch, api/dates, AdminPages | Bỏ trống/giá/100% giả; giá thiếu hiển thị chưa cấu hình; report chưa có API disabled; kỳ báo cáo động; chỉ số đúng tên; ngày VN | Filter/sort accent/null-price/date tests; render không có mock price/status | FIXED & TESTED |
| F18 | Medium | QuanLySanController | Ô có booking dùng DatSan.gia, ô chưa đặt dùng GiaSan.gia | HTTP đổi bảng giá thành mức khác rồi matrix vẫn trả snapshot booking | FIXED & TESTED |
| F19 | Đã có quyết định | NhanVienDatSanController, StaffOnsitePages | Khoảng check-in được xác nhận 30 phút trước đến kết thúc; đúng state; không quảng cáo camera QR chưa có | HTTP check-in trong cửa sổ PASS; đơn tương lai xa 409 | FIXED & TESTED |
| F20 | Low | legacy StaffPages/DangKy/QuanLySan/ProductForm/productApi, api/authApi | Giữ và ghi deprecated module có thể có consumer ngoài App; bỏ import react-router-dom thiếu dependency; product client dùng token chung; wrapper forgot trả lỗi hướng dẫn, không gọi URL giả | Lint/build và kiểm routing; Compatibility exports dùng matrix/check-in hiện hành; test SSR legacy; không còn 3 URL sai | FIXED & TESTED |
| F21 | Medium | NhanVienDatSanController, StaffOnsitePages | Walkin chỉ cash/transfer; bỏ VNPay khỏi lựa chọn và giải thích chưa hỗ trợ; Backend từ chối để không tạo payment treo | HTTP VNPay walkin 400, không booking; cash walkin 201, số tiền đúng | FIXED & TESTED |
| F22 | Medium | App, navigationConfig, ThanhToanController | Onsite có route thu tiền dùng page chung; quyền không vượt BE; confirm theo cụm và booking state | FE render route đúng role; HTTP onsite thu tiền đúng cụm/ngoài cụm | FIXED & TESTED |
| F23 | High | VNPayService, ThanhToanRepository/entity, VNPayUtil, migration | Transaction và khóa cùng sân/booking; txnRef ổn định khi retry/unique; signature constant-time, amount/merchant/status/reference checks; replay an toàn; đơn hủy/hết hạn không hồi sinh; đánh dấu xử lý thủ công | H2 signed/invalid/amount/replay/cancel/late/rollback/owner tests | FIXED & TESTED |
| F24 | Medium | MinioService, ProductService, SanImageController, SanAdminController; adminApi/AdminPages/FavoritePages | Object key riêng với URL; normalize URL bucket cũ; validate magic bytes/type/size; private bucket, report URL ký 15 phút; ảnh công khai qua sân hoạt động; admin preview ký; tải qua link sau response tránh popup | Unit object key/external URL/image payload; network MinIO/upload/report thật chưa thử | FIXED — NOT RUNTIME VERIFIED |
| F25 | Medium | api/http/staffApi/productApi/QuanLySan; SecurityConfig/CorsConfig; FE Dockerfile/nginx/CI | Client chung + base cùng origin mặc định; JSON storage an toàn; 401 clear/403 giữ phiên; giữ status/fields/text; chặn mixed content; CORS env một nơi; Nginx runtime proxy | 6 API client tests; Docker/Nginx routing mới chưa chạy container tại môi trường này | FIXED & TESTED |
| F26 | Medium | ui/props/imports/hooks, FE tests/package/CI, Backend tests/CI | Sửa lint nguyên nhân; không relax ESLint/disable hàng loạt; CI gate lint/test/build; deploy manual | lint 0/0, 11 FE tests, 26 BE tests, build PASS; chưa chạy GitLab runner thực | FIXED & TESTED |
| F27 | Low | SecurityConfig, application.yml, HealthController | H2 console tắt; giữ frame protections; Swagger mặc định tắt, bật thì admin; DB health 200/503 thực; csrf off chỉ vì Bearer stateless | Health unit valid/down, application HTTP test khởi động an toàn | FIXED & TESTED |

## Kiểm tra lại 39 chức năng

“DB” là mapping và persistence được xác minh trong H2 nếu cột Test ghi H2; không chứng nhận schema PostgreSQL hiện hữu. “FE chưa E2E” nghĩa giao diện đã tích hợp nhưng chưa chạy browser với đầy đủ API/các vai trò.

| STT / Chức năng | Frontend | Backend | API | Database | Test | Kết quả |
|---|---|---|---|---|---|---|
| 1 Đăng nhập | LoginPage, client chung | AuthController | POST dang-nhap | NguoiDung | HTTP H2 login/JWT | FIXED & TESTED — HTTP H2, browser đăng nhập Customer/Admin/2 Staff với BE QA |
| 2 Đăng xuất | App, xoaPhien | Token stateless | Không cần API logout trong thiết kế hiện hành | Không ghi | FE session-clear tests | FIXED & TESTED — browser đổi vai trò/log out; JWT stateless không revoke |
| 3 Đăng ký | RegisterPage | AuthController/UserInput | POST dang-ky | NguoiDung | HTTP H2 + invalid unit | FIXED & TESTED — HTTP đăng ký/validation; Auth Layout responsive; submit browser chưa thử |
| 4 Xác thực/phân quyền | App guard/session event | JWT filter/method security | Protected endpoints | User/role/cluster | JWT role/expired/lock + HTTP 401/403 | FIXED & TESTED — JWT unit/HTTP role, lock, scope; route regressions |
| 5 Hồ sơ | GET me/load/retry/save | NguoiDungController | GET/PUT me | NguoiDung | GET HTTP, validation unit | FIXED & TESTED — HTTP GET/PUT persistence; browser cập nhật hồ sơ chưa E2E |
| 6 Đổi mật khẩu | Form/client | BCrypt validation | PUT doi-mat-khau | NguoiDung | Invalid validation unit | FIXED & TESTED — HTTP wrong-password/BCrypt/new-login; browser chưa đổi mật khẩu |
| 7 Quên mật khẩu | Hướng dẫn liên hệ | Không có workflow recovery | Wrapper không gọi URL sai | Không | Build/source | NOT STARTED — recovery backend chưa có; giữ hướng dẫn liên hệ, không gọi URL giả |
| 8 Sân công khai | PremiumHomePage | Active-only DTO | GET san | San/GiaSan | HTTP active/inactive + browser error | FIXED & TESTED — API active-only và browser danh sách populated QA |
| 9 Tìm/lọc/sort sân | Hero search thật | Dùng danh sách sân | GET san + lọc client | Dữ liệu sân | FE accent/cluster/type/price test | FIXED & TESTED — browser lọc Sân 7/sort giá; unit dấu tiếng Việt/cluster/null-price |
| 10 Lịch trống | FieldDetailPage/retry | Shared overlap/expiry | GET lich-trong | DatSan/GiaSan | H2 guard expiry/overlap | FIXED & TESTED — browser lịch thực, khung đã qua disabled; API guard |
| 11 Online cash booking | FieldDetailPage | Transaction/locks | POST dat-san | DatSan/ThanhToan | HTTP + H2 | FIXED & TESTED — browser tiền mặt → lịch sử → staff duyệt → thu tiền |
| 12 Chống trùng | FE lịch hỗ trợ | Field/slot locks + overlap | Online/walkin | DatSan | H2 hai thread + overlap/adjacent | FIXED & TESTED — H2 booking concurrency; PostgreSQL staging chưa thử |
| 13 Hủy đơn | History action | Transaction/payment state | PUT huy | DatSan/ThanhToan | H2 paid/unpaid + HTTP confirm canceled | FIXED & TESTED — API hủy/không confirm đơn hủy; browser hủy chưa E2E |
| 14 Lịch sử | Reload/reset error | Customer-owned DTO | GET cua-toi | DatSan | HTTP flow đọc hóa đơn; source history | FIXED & TESTED — browser lịch sử có đơn đã tạo; API owner |
| 15 Yêu thích | Home/Favorites client | User-scoped transaction | GET/POST/DELETE yeu-thich | YeuThich | Build/source | FIXED & TESTED — browser thêm → Favorites → bỏ; HTTP user scope/idempotent; guest prompt/mobile |
| 16 CRUD cụm | Load/error/retry/save | CRUD guards | admin/cum-san | CumSan | Source; admin unit liên quan cụm | FIXED & TESTED — HTTP CRUD cụm/xóa khi còn staff 409; browser CRUD cụm chưa E2E |
| 17 Phân công cụm | Admin assignment | Scope được áp dụng | assign + staff endpoints | NguoiDung.cumSan | HTTP ngoài cụm denied | FIXED & TESTED — HTTP assignment/FK/scope; browser phân công chưa E2E |
| 18 Quản trị sân | Danh sách admin riêng, upload/save | CRUD/trạng thái/guard | admin/san | San | Unit đổi cụm/null; HTTP listing/delete | FIXED & TESTED — browser GET/POST/PUT/trạng thái/tìm/lọc/DELETE; đổi cụm HTTP; upload chỉ mock |
| 19 Loại sân | Select hiện hành | Chuỗi loaiSan | Trong sân | San | Source | FIXED — NOT RUNTIME VERIFIED — select/type validation, không đổi dữ liệu cũ hoặc tạo danh mục |
| 20 Khung giờ | Loading/retry/create/edit/delete | Chặn xóa/sửa lịch sử, khóa | khung-gio | KhungGio | HTTP delete-history409 | FIXED & TESTED — HTTP CRUD/overlap/adjacent/history guard; browser create/edit/delete |
| 21 Bảng giá | CRUD form | Giá >0/type/duplicates | gia-san | GiaSan | HTTP snapshot sau đổi giá | FIXED & TESTED — HTTP CRUD/duplicate/validation/snapshot; browser 315k → 325k → delete |
| 22 Duyệt/từ chối | Staff requests | State/scope/transaction | duyet/tu-choi | DatSan/ThanhToan | HTTP approve + source reject | FIXED & TESTED — HTTP approve/reject/payment cancellation; browser approve |
| 23 Ma trận | Inactive labels/booking details | Scope/overlap/snapshot | lich-ma-tran | San/GiaSan/DatSan | HTTP snapshot | FIXED & TESTED — HTTP snapshot; full matrix interaction browser chưa nghiệm thu |
| 24 POS/check-in | Tìm/submit, không camera claim | 30-minute window/scope | tim-kiem/checkin | DatSan | HTTP search/scope/window | FIXED & TESTED — HTTP valid window/scope; browser search/future-check-in denied |
| 25 Walkin | Cash/transfer, không VNPay giả | Transaction + guard | POST walkin | User/DatSan/Payment | HTTP cash success/VNPay rejection | FIXED & TESTED — HTTP cash/transfer; browser Walkin transfer; VNPay giữ disabled |
| 26 Thu tiền | Onsite/admin shared route | Scope/state/locks | quan-ly/xac-nhan | ThanhToan | HTTP paid amount/outsider/canceled | FIXED & TESTED — browser xác nhận cash và manual transfer; HTTP canceled/scope/idempotent |
| 27 Hóa đơn | History modal | Ownership/staff scope | GET hoa-don | Booking/payment | HTTP invoice amount | FIXED & TESTED — HTTP invoice ownership/amount; modal mới, browser invoice chưa E2E |
| 28 VNPay | Redirect/link existing | Signature/state/idempotency | POST create, GET return | Booking/payment | 7 H2 booking/payment tests liên quan | WAITING FOR INSTRUCTOR — freeze toàn bộ logic/config/schema VNPay; chỉ chạy regression cũ, không gọi gateway |
| 29 Dashboard | Loading/error/retry | Paid transactions | GET dashboard | Booking/payment/user | Build/source, HTTP role deny | FIXED & TESTED — browser Dashboard real QA; charts actual API; HTTP staff count |
| 30 Doanh thu | VN dates/correct labels | Paid sum theo thời điểm | GET doanh-thu | ThanhToan | HTTP sum + FE date tests | FIXED & TESTED — HTTP tổng tiền + date tests; browser bộ lọc doanh thu chưa E2E |
| 31 Excel | Click-to-download signed link | POI + MinIO | GET bao-cao/excel | Payments + storage | Compile/source | BLOCKED — chưa MinIO staging upload/download/Excel thật |
| 32 PDF | Click-to-download signed link | iText + MinIO | GET bao-cao/pdf | Payments + storage | Compile/source | BLOCKED — chưa MinIO staging PDF/font/download thật |
| 33 Loại report chưa có API | Options disabled | Không tự thêm | Không | Không | Source | NOT STARTED — loại báo cáo thiếu API vẫn disabled, không tạo dữ liệu giả |
| 34 Products demo | Deprecated, không route mới | Existing CRUD/image | products | Product | MinIO object-key unit/source | FIXED — NOT RUNTIME VERIFIED — module legacy giữ nguyên, không route mới |
| 35 Nhân viên | Load/error/retry/save guard | Role/email/lock validation | admin/nhan-vien | NguoiDung | User validation + lock tests | FIXED & TESTED — HTTP create/update/delete staff; validation/lock; browser CRUD staff chưa E2E |
| 36 Khách hàng | Load/error/retry/save guard | Validation/lock/FK409 | admin/khach-hang | NguoiDung | Register/lock HTTP tests | FIXED & TESTED — HTTP create/update/lock/delete customer; browser CRUD customer chưa E2E |
| 37 Avatar | Local file/validation/notice | Không thêm upload | Không | Chỉ browser | Source | FIXED — NOT RUNTIME VERIFIED — avatar chỉ local, không tuyên bố đồng bộ DB |
| 38 Staff legacy | Compatibility exports | API hiện hành | Matrix/check-in hiện hành | Không thêm workflow | SSR/build | FIXED & TESTED — compatibility exports qua màn hình hiện hành; SSR, không còn URL sai |
| 39 Health | API health wrapper | DB connection validation | GET health | Kiểm DB thiết yếu | 2 tests UP/DOWN | FIXED & TESTED — DB health UP/DOWN unit; QA server dùng H2, không giả MinIO/VNPay health |

## API/Database thay đổi đáng chú ý

- Thêm `GET /api/admin/san` cho danh sách quản trị đầy đủ. FE quản trị/giá chuyển sang endpoint này; public `GET /api/san` chỉ active. Đây là bước ngăn regression sân ngừng biến mất khỏi quản trị.
- Thêm `GET /api/san/{id}/image` cho ảnh sân hoạt động, lấy key trong DB, không cho truyền key tùy ý. Thêm `GET /api/admin/san/{id}/image-url` cho admin preview ảnh kể cả sân tạm ngưng.
- Upload ảnh sân trả thêm `objectKey` bên cạnh `url` ký thời hạn; FE lưu key, preview dùng URL; không lưu URL hết hạn vào DB. Báo cáo Excel/PDF tiếp tục trả `{url}`, URL ký thay vì public bucket.
- Booking/payment methods giữ URL/body cũ; sửa ownership/scope/time/status và locks. Return không cần Bearer nhưng truy cập rỗng/sai signature không được thành công. **Chưa triển khai IPN giao thức riêng**; cấu hình và nghiệm thu callback với tài khoản VNPay sandbox còn phải thực hiện trước rollout.
- Thanh toán DTO thêm `tongTien` và `canXuLyHoanTien`. Cờ được lưu khi hủy đơn đã trả hoặc callback đã ký đến muộn/đơn hủy; không tự chuyển tiền, không hồi sinh booking. Cần quy trình xử lý thủ công/đối soát riêng.
- Không hard-delete booking/payment qua cascade khi xóa sân/khung giờ/cụm; trường hợp còn FK trả 409. Hệ thống mới dùng cùng quy tắc thời gian VN cho đặt/hủy/check-in/thanh toán/doanh thu.
- Migration chuẩn bị: [20261012_booking_payment_safety.sql](<D:/Năm 3/RaiWay99/QuanLiSanBong/database/migrations/20261012_booking_payment_safety.sql>). Thêm cột boolean refund-review và unique index VNPay reference, có kiểm duplicate trước tạo index, không tự sửa/xóa duplicate. File **không được tự chạy** và chưa kiểm trên PostgreSQL staging. App mặc định schema validate; schema thật cần được đối chiếu trước triển khai.

## Giao diện Premium và responsive

HomePage tách thành [PremiumHomePage.jsx](<D:/Năm 3/RaiWay99/QuanLiSanBong/fe/src/pages/PremiumHomePage.jsx>), CSS tại [Homepage.css](<D:/Năm 3/RaiWay99/QuanLiSanBong/fe/src/pages/Homepage.css>). Hero xanh forest, typography lớn và hình bóng đá từ nguồn đã có trong source; chỉ một ảnh eager thay carousel tải nhiều ảnh. Search bar lọc tên/cụm/loại từ GET thật; không bịa tìm lịch theo ngày/giờ trên toàn bộ sân khi chưa có API phù hợp. Ngày/giờ chọn trong chi tiết sân.

Cards có real price/unknown-price state, không fake rating/availability; thiếu ảnh có placeholder. Skeleton/error/retry/empty state, hover/focus, reveal nhẹ và reduced-motion. Quy trình ba bước, lợi ích có cơ sở chức năng, CTA và footer mới. Header sticky/mobile navigation; vẫn giữ **ẩn nút header “Đặt sân” trên home** và giữ Hero CTA cùng hành động đặt sân hợp lệ.

| Viewport | Document scrollWidth đo được | Tràn ngang | Header booking nav |
|---|---|---|---|
| 1920px | 1905px | Không | Không xuất hiện |
| 1440px | 1425px | Không | Không xuất hiện |
| 1024px | 1009px | Không | Không xuất hiện |
| 768px | 753px | Không | Không xuất hiện |
| 390px | 375px | Không | Không xuất hiện |
| 375px | 360px | Không | Không xuất hiện |

Chênh lệch width là scrollbar của browser. Đã nhìn screenshot desktop và mobile; Hero/search không tràn hoặc chồng chữ. Browser local QA cố ý không nối Backend production: API danh sách trả sai định dạng từ local static server, page hiển thị lỗi thật và retry, không chèn mock để giả danh sách sân. **Chưa kiểm browser cards với danh sách nhiều sân thật, toàn bộ role và E2E đầy đủ.** Viewport tạm thời đã reset sau kiểm tra.

Ảnh kiểm tra: [Desktop](<D:/Năm 3/RaiWay99/QuanLiSanBong/target/home-desktop.png>), [Mobile](<D:/Năm 3/RaiWay99/QuanLiSanBong/target/home-mobile.png>). Lưu trong target, có thể bị xóa khi chạy Maven clean.

## Build, lint và test

| Kiểm tra | Lệnh | Kết quả/giới hạn |
|---|---|---|
| FE lint | `cd fe; npm run lint` | PASS 0 errors/0 warnings, không tắt rule |
| FE tests | `cd fe; npm test` | 11 PASS: client/session/errors/params/text/multipart, VN calendar, filtering/sorting, rendering/route guards/header/no mock |
| FE production build | `cd fe; npm run build` | PASS 61 modules; không chứng minh live API |
| Backend build/test | Maven IntelliJ + `-Dmaven.repo.local=.m2/repository -o test` | PASS 26 tests; 80 main source và 7 test source compile |
| BookingIntegrationTest | 7 tests, H2/JPA + transaction | Overlap/adjacent/past/inactive/concurrent/expiry/owner/amount/signature/replay/cancel/rollback |
| SystemHttpIntegrationTest | 4 tests, full Spring context + MockMvc/H2, MinioService mock | Register/login/role/lock; cash booking→approval→payment→check-in→invoice→revenue; matrix snapshot; deletion guards; cancel; outside cluster; inactive admin visibility; walkin |
| AdminSafetyTest | 3 tests | Change cluster/null/not-found, refuse delete with history, user validation |
| AuthAndUserValidationTest | 4 tests | Auth/register validation/duplicate/missing fields |
| HealthControllerTest | 2 tests | Valid DB UP, DB failure DOWN/503 |
| JwtSecurityTest | 4 tests | Valid/tampered/expired/weak config/locked same token/DB roles |
| MinioSafetyTest | 2 tests | Old full URL→key, external reference rejection, invalid image rejected before network |
| Browser smoke | localhost:4318, actual React | 6 viewport checks, Hero CTA scroll, retry updates page, login navigation/empty-input validation |
| Diff whitespace | `git diff --check` in both repositories (CRLF aware) | PASS after EOF cleanup |

Command cụ thể Backend trong môi trường này:

```powershell
& 'C:/Program Files/JetBrains/IntelliJ IDEA 2026.1/plugins/maven/lib/maven3/bin/mvn.cmd' '-Dmaven.repo.local=.m2/repository' -o test
```

Evidence: [full-test.log](<D:/Năm 3/RaiWay99/QuanLiSanBong/target-final-verify.log>) và `target/surefire-reports/TEST-*.xml`. Lần đầu thiếu provider Maven/DNS sandbox, lần tiếp theo tải dependency với quyền network rồi test PASS; không giấu lỗi đó thành pass ban đầu. Các test nguồn sử dụng dữ liệu QA và khóa kiểm thử riêng, không đọc credentials production. Thông báo wrapper “Access is denied.” có trước Maven nhưng không cản stages/exit cuối; kết quả dựa vào XML và Maven BUILD SUCCESS, không dựa vào wrapper.

## Các giới hạn còn lại và trước triển khai

1. **PostgreSQL staging:** kiểm schema/FK/unique/migration và cạnh tranh trên PostgreSQL thật bản staging, gồm nhiều request hơn hai thread và các thao tác admin đồng thời. H2 PASS không thay thế xác minh PostgreSQL. Migration chưa áp dụng, production không bị tác động.
2. **VNPay:** cần tài khoản/cấu hình sandbox hợp lệ để kiểm return/IPN hợp đồng gateway, amount/status/timezone/URL ngoài localhost và đối soát. Không gọi gateway thật trong lần này; chưa triển khai giao thức IPN riêng. Cờ xử lý hoàn tiền không phải workflow hoàn tiền hoàn chỉnh.
3. **MinIO:** kiểm bucket private, mạng/HTTPS và signed URL có thể truy cập từ browser; thử file thật upload/download/Excel/PDF/font. URL cũ trong bucket có normalize; URL ảnh ngoài bucket không được proxy tùy ý. Ảnh trường sân tạm ngưng dùng admin signed preview, public stream từ chối.
4. **Frontend QA còn thiếu:** đã thêm guard đồng bộ cho save/delete/toggle/assign/favorite/check-in/booking và xử lý checkbox ghi nhớ đăng nhập theo session/local storage; chưa có DOM/browser E2E đầy đủ các form và nhiều thao tác khác nhau đồng thời. Không xác nhận F14 runtime toàn diện.
5. **Legacy:** StaffPages giữ tên export qua màn hình hiện hành; đã bỏ ba URL không tồn tại. Các module product/recovery/report chưa có nghiệp vụ hoàn chỉnh vẫn ghi rõ giới hạn, không quảng cáo đã triển khai.
6. **Secrets đã từng xuất hiện trong source/history:** code mới không chứa credentials thật. Người quản trị phải đánh giá/xoay vòng khóa đã lộ ở hệ thống thật khi phù hợp; agent không tự xoay vòng. File .env.example không tự được Spring Boot load, cần export hoặc Docker env-file/CI secret file.
7. **Deployment:** CI build/lint/tests đã được cấu hình, deploy chuyển manual ở hai repo. Cần `APP_ENV_FILE`, `DOCKER_NETWORK`, `API_UPSTREAM` và HTTPS endpoint/proxy ở staging trước chạy. Nginx template dùng upstream runtime trên shared Docker network; Docker/Nginx/GitLab runner chưa được thực thi tại môi trường này. Frontend không dùng localhost mặc định trong production image.
8. **Lịch sử và dữ liệu cũ:** không mass-update loại sân/username/email; schema username 50 vẫn được giữ và validation ngăn input vượt giới hạn. Không bỏ giá hoặc dữ liệu lịch sử để đạt test. Đổi lịch, hoàn thành đơn tự động, danh mục loại sân riêng, recovery và full refund không được tự xây mới.

Không còn lỗi lint hoặc test regression phát hiện trong phạm vi các bài đã chạy. Không tuyên bố toàn bộ 39 chức năng đã hoàn thành/E2E hoặc mọi rủi ro production đã được loại bỏ. Ưu tiên tiếp theo: nghiệm thu PostgreSQL staging + migration → VNPay/MinIO sandbox → browser E2E từng vai trò → người dùng duyệt triển khai.

## Danh sách file thay đổi

Danh sách dưới đây được lấy từ hai repository và các file source/test mới; không bao gồm cache/build artifact hoặc package-lock.json cha đã thay đổi từ trước.
- `.env.example`
- `.gitignore`
- `.gitlab-ci.yml`
- `database/migrations/20261012_booking_payment_safety.sql`
- `src/main/java/com/example/becicd/config/CorsConfig.java`
- `src/main/java/com/example/becicd/config/RequiredConfiguration.java`
- `src/main/java/com/example/becicd/config/SanBongDataSeeder.java`
- `src/main/java/com/example/becicd/config/SanDataInitializer.java`
- `src/main/java/com/example/becicd/config/ScheduleDataInitializer.java`
- `src/main/java/com/example/becicd/config/SecurityConfig.java`
- `src/main/java/com/example/becicd/controller/AdminDashboardController.java`
- `src/main/java/com/example/becicd/controller/AdminUserController.java`
- `src/main/java/com/example/becicd/controller/AuthController.java`
- `src/main/java/com/example/becicd/controller/CumSanAdminController.java`
- `src/main/java/com/example/becicd/controller/DatSanController.java`
- `src/main/java/com/example/becicd/controller/HealthController.java`
- `src/main/java/com/example/becicd/controller/KhungGioController.java`
- `src/main/java/com/example/becicd/controller/NguoiDungController.java`
- `src/main/java/com/example/becicd/controller/NhanVienDatSanController.java`
- `src/main/java/com/example/becicd/controller/ProductController.java`
- `src/main/java/com/example/becicd/controller/QuanLySanController.java`
- `src/main/java/com/example/becicd/controller/SanAdminController.java`
- `src/main/java/com/example/becicd/controller/SanController.java`
- `src/main/java/com/example/becicd/controller/SanImageController.java`
- `src/main/java/com/example/becicd/controller/SanPublicController.java`
- `src/main/java/com/example/becicd/controller/ThanhToanController.java`
- `src/main/java/com/example/becicd/controller/VNPayController.java`
- `src/main/java/com/example/becicd/dto/UserInput.java`
- `src/main/java/com/example/becicd/entity/CumSan.java`
- `src/main/java/com/example/becicd/entity/DatSan.java`
- `src/main/java/com/example/becicd/entity/KhungGio.java`
- `src/main/java/com/example/becicd/entity/San.java`
- `src/main/java/com/example/becicd/entity/ThanhToan.java`
- `src/main/java/com/example/becicd/exception/GlobalExceptionHandler.java`
- `src/main/java/com/example/becicd/repository/DatSanRepository.java`
- `src/main/java/com/example/becicd/repository/KhungGioRepository.java`
- `src/main/java/com/example/becicd/repository/NguoiDungRepository.java`
- `src/main/java/com/example/becicd/repository/SanRepository.java`
- `src/main/java/com/example/becicd/repository/ThanhToanRepository.java`
- `src/main/java/com/example/becicd/security/JwtAuthFilter.java`
- `src/main/java/com/example/becicd/security/JwtTokenProvider.java`
- `src/main/java/com/example/becicd/service/BookingGuard.java`
- `src/main/java/com/example/becicd/service/MinioService.java`
- `src/main/java/com/example/becicd/service/ProductService.java`
- `src/main/java/com/example/becicd/service/VNPayService.java`
- `src/main/java/com/example/becicd/util/VNPayUtil.java`
- `src/main/resources/application.yml`
- `src/test/java/com/example/becicd/BookingIntegrationTest.java`
- `src/test/java/com/example/becicd/controller/AdminSafetyTest.java`
- `src/test/java/com/example/becicd/controller/AuthAndUserValidationTest.java`
- `src/test/java/com/example/becicd/controller/HealthControllerTest.java`
- `src/test/java/com/example/becicd/security/JwtSecurityTest.java`
- `src/test/java/com/example/becicd/service/MinioSafetyTest.java`
- `src/test/java/com/example/becicd/SystemHttpIntegrationTest.java`
- `fe/.gitlab-ci.yml`
- `fe/Dockerfile`
- `fe/index.html`
- `fe/nginx.conf`
- `fe/package.json`
- `fe/src/api/adminApi.js`
- `fe/src/api/authApi.js`
- `fe/src/api/dates.js`
- `fe/src/api/http.js`
- `fe/src/api/productApi.js`
- `fe/src/api/sanApi.js`
- `fe/src/api/staffApi.js`
- `fe/src/App.jsx`
- `fe/src/components/GuestLayout.jsx`
- `fe/src/components/Layout.jsx`
- `fe/src/components/navigationConfig.js`
- `fe/src/components/ProductForm.jsx`
- `fe/src/components/QuanLySan.jsx`
- `fe/src/components/StaffScheduleMatrix.jsx`
- `fe/src/components/ui.jsx`
- `fe/src/index.css`
- `fe/src/pages/AdminOperationsPages.jsx`
- `fe/src/pages/AdminPages.jsx`
- `fe/src/pages/DangKy.jsx`
- `fe/src/pages/FavoritePages.jsx`
- `fe/src/pages/GuestPages.jsx`
- `fe/src/pages/Homepage.css`
- `fe/src/pages/PremiumHomePage.jsx`
- `fe/src/pages/StaffOnsitePages.jsx`
- `fe/src/pages/StaffPages.jsx`
- `fe/src/utils/courtSearch.js`
- `fe/tests/client.test.cjs`
- `fe/tests/render.test.cjs`

## Đợt tiếp tục: F14/F20 và xuất bản GitLab (12/10/2026)

- F14: chặn đồng thời cùng action bằng khóa đồng bộ, giữ khóa qua reload; checkbox ghi nhớ chọn localStorage/sessionStorage, cập nhật hồ sơ giữ lựa chọn, logout/401 xóa cả hai; test hành vi cho guard và phiên. Tải cấu hình lịch đợi cả sân/khung giờ, bỏ kết quả giá cũ khi đổi sân; VNPay link thất bại sau tạo đơn hướng về lịch sử. Chưa có DOM/E2E đầy đủ mọi thao tác, nên không xác nhận F14 runtime toàn diện.
- F20: giữ tên MatrixCalendarPage/OnsitePosPage cho consumer cũ qua exports sang màn hình hiện hành, loại bỏ ba URL không tồn tại; kiểm thử render legacy.
- Seed dev yêu cầu DEMO_PASSWORD >=12 ký tự từ môi trường, bỏ mật khẩu demo cố định. Không seed trên production.
- FE: lint 0 lỗi/0 cảnh báo; 14 test; build Vite. BE: Maven offline clean verify, 26 test không fail/error/skipped, đóng gói executable JAR. BE không có cấu hình lint riêng; dùng compiler, tests và git diff --check, không tuyên bố đã chạy Java linter.
- Fetch GitLab cả hai thành công. HEAD BE update-backend-0926 trùng origin/update-backend-0926; origin/main chứa cùng source. FE main có thể fast-forward lên origin/main mới, cây source giống HEAD. Không ghi đè remote.
- Quét toàn bộ file tracked và untracked không ignore của hai repo: không phát hiện private key, token nhà cung cấp, URL có credential hay literal sensitive assignment. Chỉ dùng cấu hình env placeholders và fixture test; không đưa .env production, cache, logs, build output vào commit. Secret có trong lịch sử đã có trên remote không được coi là đã xóa khỏi lịch sử; không rewrite lịch sử.
- API GitLab với credential Git hiện có trả 403 khi đọc project/CI. Git fetch vẫn hoạt động; tiếp tục thử push và xác minh SHA bằng ls-remote. Không coi CI là PASS nếu chưa đọc được.
- package-lock.json trống ở repo cha là thay đổi có sẵn không thuộc ứng dụng Maven: giữ nguyên tại local và không commit.

### Kết quả xuất bản đã xác minh

| Repository | Branch remote | Commit source đã push | Merge Request | Xác minh |
|---|---|---|---|---|
| Frontend | codex/full-fix-20261012 | 22ede6c525ba62aa281ec9dd60ec9ab9ad64ac0f | [FE !5](https://gitlab.com/maiiilinhhh220204-group/quanlysanbong/fe/-/merge_requests/5) | git ls-remote khớp HEAD; working tree sạch |
| Backend | update-backend-0926 | 1426799b251085184518dd7b07d585dfe269a1a9 | [BE !5](https://gitlab.com/maiiilinhhh220204-group/quanlysanbong/be/-/merge_requests/5) | git ls-remote khớp HEAD; gitlink fe trỏ commit FE đã push |

FE main bị GitLab từ chối do protected branch: đã tạo feature branch và MR thành công bằng Git push options. BE branch hiện hành được chấp nhận, upstream chuyển sang origin GitLab đúng yêu cầu. Không push GitHub, không force, không merge MR, không chạy deploy manual. Báo cáo này được đưa vào commit tài liệu tiếp theo trên BE; SHA cuối xem Git history, tránh ghi SHA tự tham chiếu vào chính commit.

**CI/CD: CHƯA XÁC MINH được trạng thái pipeline remote.** GitLab REST trả 403 cho cả project FE/BE với credential Git hiện có; công cụ CI connector từ chối truy cập workspace; browser GitLab chuyển sang trang Sign in và không có phiên đăng nhập. Không có pipeline ID/status/log để kết luận PASS hay FAIL. Push thành công không được đánh đồng với CI thành công. CI đã cấu hình build/lint/tests FE và compile/tests BE; chưa thực thi runner/Docker/deploy thật trong phiên này.

Git status sau commit source: FE sạch; BE chỉ còn package-lock.json trống có sẵn, đã cố ý giữ local và không đưa vào commit. Cache Maven, node_modules, dist, target và log verify đều ignore. Scan pattern không thay thế việc xoay vòng credential từng có trong lịch sử remote; không công bố giá trị đó trong báo cáo.

**Chưa kiểm thử môi trường thật:** PostgreSQL/migration và khóa cạnh tranh trên PostgreSQL; VNPay gateway/return/IPN sandbox; MinIO signed URL/upload/download, Excel/PDF/font; full browser E2E mọi vai trò; Docker/Nginx/runner/staging deploy. Giới hạn F14 là coverage DOM/E2E, không còn lỗi checkbox/ba URL legacy đã xác định.

## Đợt 13/10/2026 — Premium Football Club, API quản trị và navigation

Đây là kết quả mới nhất. Bảng 39 chức năng phía trên đã được cập nhật theo source/test hiện tại; các số liệu screenshot/build/test 12/10 phía trước là lịch sử, không phải kết quả mới. Không coi coverage HTTP/H2 là chứng nhận production/E2E toàn bộ.

### Lỗi thực tế và nguyên nhân 405

1. FE gọi **GET /api/admin/san**, POST thêm, PUT sửa/trạng thái, DELETE xóa đúng với controller hiện tại. Source trước đợt sửa 12/10 không có GET quản trị; source baseline mới đã có @GetMapping. Port **8084 không có tiến trình lắng nghe khi kiểm tra**, nên không tái hiện được phiên Backend mà người dùng đã thấy 405. Trạng thái 405 trên source hiện tại: **NOT REPRODUCIBLE**. Không khẳng định lỗi runtime cũ đã biến mất ở máy/staging chưa kiểm tra. GET trong MockMvc và browser QA 8085 trả dữ liệu admin, không dùng API public thay thế.
2. CRUD sân: POST chưa kiểm độ dài tên/loại theo schema; bổ sung trim/tên <=100/loại <=20. FE không còn nuốt thông điệp lỗi, từ chối response danh sách sai kiểu; 405 có hướng dẫn kiểm bản Backend/API URL; nút refresh/retry gọi API thật. Lịch sử/xóa có guard/FK giữ nguyên. Ảnh không có dùng placeholder trung tính; upload signed URL/objectKey chỉ kiểm contract bằng MinioService mock, không giả upload MinIO thật.
3. Khung giờ: PUT đã có nhưng thiếu nút sửa trên FE. Đã nối editor vào PUT và reload, giữ chặn sửa/xóa khung có lịch sử. Backend chặn khoảng mới giao nhau, cho phép giờ liền kề. Input giờ cần onInput + functional state update để giá trị đi vào React state trên browser QA. Các khung là lịch lặp theo giờ, không mang ngày: không áp đặt quy tắc cấm giờ trước hiện tại cho master schedule; booking có guard ngày/giờ như trước. Không sửa hàng dữ liệu cũ.
4. Bảng giá: **min=1 + step=1000** khiến giá bình thường 315000 không hợp lệ (HTML chỉ chấp nhận 314001/315001). Đổi step=1, giữ min=1, xác minh 315000 → 325000 → xóa qua browser. HTTP kiểm giá 0/trùng/type/CRUD; giá booking lịch sử vẫn dùng snapshot. Backend đã hỗ trợ transfer Walkin nhưng FE chỉ có cash: bổ sung lựa chọn chuyển khoản thủ công vào đúng API hiện hành, giao dịch chờ thu, không thêm tích hợp ngân hàng.
5. Dashboard: count nhân viên từng bao gồm ADMIN; loại ADMIN khỏi count staff, HTTP fixture kỳ vọng đúng 3 nhân viên. KPI/line chart/donut dùng dashboard + danh sách sân quản trị thật; không có % tăng trưởng hay lượng đặt giả. UI nhãn tuần không bị gọi thành tổng toàn thời gian.

### VNPay — WAITING FOR INSTRUCTOR

So sánh với baseline BE 04a42d2 / FE 22ede6c: VNPayService, VNPayController, VNPayConfig, VNPayUtil, application.yml, migration/schema, ThanhToanController, BookingGuard, DatSanController và NhanVienDatSanController không đổi. Hai handler FE handleDatSan/tiepTucThanhToan giữ nguyên từng dòng; authApi/datSanApi/http không đổi logic. Các test callback/merchant/signature cũ giữ nguyên và chạy regression; tests mới chỉ phục vụ API quản trị, yêu thích, hồ sơ và phương thức manual. Fixture QA có cấu hình dummy để context khởi động, không gọi gateway hay đọc production secret.

Đang chờ giảng viên: gateway sandbox chưa kiểm; SecurityConfig có permit IPN nhưng VNPayController không có handler IPN riêng. Không bổ sung/chỉnh chữ ký, return, merchant, URL hay transaction reference. Không đánh dấu tích hợp VNPay hoàn tất.

### Giao diện và navigation

- Design tokens #DA1A32 / #B51228 / #0B0F14 / #151B23 / #FFFFFF / #F5F6F8 / #E5E7EB / #64748B; xanh dùng cho trạng thái phù hợp. PublicLayout, Brand, CourtCard, AuthLayout/PasswordInput, DialogShell, DashboardCharts dùng chung. Animation nhẹ, focus visible, reduced motion; modal/drawer Escape + focus trap/return focus; nhãn admin input liên kết htmlFor/id.
- Trang chủ hero stadium artwork, overlay, search thật, card sân, quy trình/benefits/footer; `/tim-san` dùng cùng dữ liệu/API và card, không tạo backend tìm kiếm mới. Chi tiết sân thêm metadata/ảnh/yêu thích, giữ booking flow. Favorites dùng card chung, guest prompt, trạng thái API. Login/Register cùng Auth Layout, toggle cả password/confirm, validation/loading/error. Profile/history/invoice đồng bộ màu, bảng có cuộn nội bộ. Dashboard và tất cả layout Admin/Staff/CRUD/report/matrix/POS/Walkin đồng bộ dark sidebar, red active/CTA và form/table/modal.
- Navigation theo thứ tự **Trang chủ → Tìm sân → Sân nổi bật → Sân yêu thích → Về chúng tôi**. Sân nổi bật là `/#courts` thật; Về chúng tôi là section `/#about` mô tả nền tảng thật. **Tin tức chưa có route/API/nội dung nên không hiển thị**, không thêm trang giả. Không có ranking/rating giả hoặc claim sân còn trống khi chưa chọn lịch.
- Đã xóa JSX nút Header "Đặt sân"/nav-pill ở Guest/Customer mọi trang, xóa hideBookingNav và CSS nav-pill. Hero CTA "Đặt sân ngay", route `/san/{id}`, API booking và history giữ nguyên. Navigation dùng href thật/aria-current, mobile hamburger có Favorites và Login; Account có history/profile/logout.
- Ảnh `fe/public/assets/stadium-hero.png` được tạo bằng built-in imagegen, dùng cho brand atmosphere, không làm ảnh đại diện sân từ DB. Prompt/provenance nằm ở `fe/ASSETS.md`; SVG logo bóng đá/placeholder do code tạo, không logo MU. Không thêm mock dataset vào production. Fixtures QA chỉ nằm test/H2.

### Bằng chứng kiểm thử mới

- **FE:** lint 0 lỗi/0 cảnh báo; **19 tests** client/SSR/component/route/price validation; Vite build. SSR không được báo thành DOM tests. Browser QA thực hiện tương tác DOM với API Spring/H2.
- **BE:** bộ mặc định **35 tests** (13 SystemHttpIntegrationTest + 22 unit/JPA khác), full security + MockMvc/H2, test schema create-drop cô lập. BrowserQaHarness là helper opt-in, không nằm trong default *Test; không cộng helper chờ server thành business test.
- Browser FE `http://127.0.0.1:4319` và BE `http://127.0.0.1:8085`, H2 memory: Guest/Favorites login prompt; Customer login không ghi nhớ, search Sân 7/sort giá, favorite add→list→remove, chi tiết→cash booking→history; Staff Page duyệt; Onsite thu cash, search POS và nhận lỗi ngoài 30-minute window; Admin list→create→edit/type/clear cluster→toggle→search/filter→safe delete; schedule create 22–23→edit21–22→delete sau xóa giá; price315k→325k→delete; Walkin transfer→pending collection→manual confirm. Không thực hiện VNPay.
- Browser Dashboard sau thu chuyển khoản QA: doanh thu tuần **0.23 triệu**, một đơn, tỷ lệ loại sân từ 4 sân QA (2 Sân5/2 Sân7), không dữ liệu giả hardcode trong UI. QA server đã được khởi động lại khi phiên công cụ cũ bị ngắt; H2 cũ bị hủy, dữ liệu mới vẫn chỉ QA.
- Đã đo **1920/1440/1024/768/390/375**, không overflow toàn trang ở Home, Search, Field detail, Login, Register, Dashboard, Admin fields, Staff payments. Bảng rộng cuộn trong container. Kiểm mobile menu Favorites, sidebar và modal; Escape trả focus đúng. Không khẳng định mọi trạng thái từng trang có chứng nhận accessibility; contrast/focus/semantic thực hiện trong code, chưa full assistive-technology audit.
- Screenshot desktop/mobile lưu ngoài Git ở `C:/Users/quang/.codex/visualizations/2026/10/12/01a13039-31bd-75f3-9750-38b00e0a40ca/redesign-home-desktop.jpg` và `redesign-home-mobile.jpg`.

### Giới hạn và việc chưa thể nghiệm thu

**BLOCKED / FIXED — NOT RUNTIME VERIFIED:** PostgreSQL staging/FK/schema/migration/concurrency thực; MinIO private bucket upload/download, Excel/PDF/font; Docker/Nginx/runner/deploy/HTTPS thật; browser submit registration/profile/change-password/full customer/staff CRUD và từng matrix cell chưa full E2E. API registration/profile/password/admin user CRUD/assignment đã được test HTTP. Lịch cạnh tranh tạo/sửa khung từ nhiều admin/nhiều node chưa có kiểm thử PostgreSQL và constraint DB; overlap validation hiện tại kiểm trong controller, không tuyên bố khóa phân tán. Giá concurrency theo master data chưa nghiệm thu staging. Không apply migration, không sửa dữ liệu production.

**NOT STARTED:** recovery backend, Tin tức, report loại chưa có API, workflow products ngoài App, avatar lưu server, full refund/đổi lịch không tự xây. **WAITING FOR INSTRUCTOR:** VNPay. Các trang đang hoạt động đã đồng bộ thiết kế; legacy không routed không được quảng cáo là workflow đã triển khai. Không tuyên bố toàn bộ 39 chức năng đã đạt full browser/staging nghiệm thu.

### Git và xuất bản

Fetch GitLab thành công, hai branch local/remote không diverge trước commit: FE `codex/full-fix-20261012`, BE `update-backend-0926`. Tiếp tục MR !5 mỗi repo, không force, không merge, không deploy. Root package-lock.json trống có sẵn tiếp tục giữ local và không commit. SHA/kết quả push/CI của đợt này sẽ được bổ sung sau xác minh remote; số SHA đợt 12/10 ở phía trước là lịch sử.

### File thay đổi đợt 13/10 và xác minh trước commit

FE đã push commit `3c388a4c9fb590b953f00c5e2bdd9f93f9afa3a2` trên `codex/full-fix-20261012`, cập nhật [FE !5](https://gitlab.com/maiiilinhhh220204-group/quanlysanbong/fe/-/merge_requests/5).

File Frontend:

- `fe/ASSETS.md`
- `fe/public/assets/court-placeholder.svg`
- `fe/public/assets/stadium-hero.png`
- `fe/src/App.jsx`
- `fe/src/club.css`
- `fe/src/components/AuthLayout.jsx`
- `fe/src/components/Brand.jsx`
- `fe/src/components/CourtCard.jsx`
- `fe/src/components/DashboardCharts.jsx`
- `fe/src/components/DialogShell.jsx`
- `fe/src/components/FieldPresentation.jsx`
- `fe/src/components/GuestLayout.jsx`
- `fe/src/components/Layout.jsx`
- `fe/src/components/PublicLayout.jsx`
- `fe/src/components/StaffScheduleMatrix.jsx`
- `fe/src/components/navigationConfig.js`
- `fe/src/components/ui.jsx`
- `fe/src/index.css`
- `fe/src/main.jsx`
- `fe/src/pages/AdminOperationsPages.jsx`
- `fe/src/pages/AdminPages.jsx`
- `fe/src/pages/BookingPage.css`
- `fe/src/pages/DangKy.jsx`
- `fe/src/pages/FavoritePages.jsx`
- `fe/src/pages/GuestPages.jsx`
- `fe/src/pages/Homepage.css`
- `fe/src/pages/LoginPage.css`
- `fe/src/pages/PremiumHomePage.jsx`
- `fe/src/pages/StaffOnsitePages.jsx`
- `fe/src/pages/StaffPagePages.jsx`
- `fe/src/utils/statusLabels.js`
- `fe/tests/client.test.cjs`
- `fe/tests/render.test.cjs`

File Backend/tài liệu (không bao gồm root package-lock.json):

- `src/main/java/com/example/becicd/controller/AdminDashboardController.java`
- `src/main/java/com/example/becicd/controller/KhungGioController.java`
- `src/main/java/com/example/becicd/controller/SanAdminController.java`
- `src/test/java/com/example/becicd/SystemHttpIntegrationTest.java`
- `src/test/java/com/example/becicd/BrowserQaHarness.java`
- `CODEX_FULL_FIX_REPORT.md`
- `fe (gitlink tới commit FE đã push)`

Quét trước commit: 103 file BE và 64 file FE (tracked + untracked không ignore, binary images không quét text); không phát hiện private key, provider token, credential URL, secret literal thật hay production env/DB dump. Fixture test chỉ dùng thông tin QA, không production credentials. Source production không import data/mock. git diff --check sạch. Root package-lock.json không stage. Backend không có Java lint riêng; compile/tests/verify chạy, không báo Java lint PASS giả. BrowserQaHarness chỉ bind 127.0.0.1, helper opt-in H2; đã dừng server QA, không deploy. Lần cuối FE build 72 modules, CSS81.14kB/JS305.50kB; BE clean verify 35 tests PASS và executable JAR. Evidence: redesign-final-verify.log (ignored), target/surefire-reports/TEST-*.xml.

GitLab CI lần kiểm tra 13/10: REST PRIVATE-TOKEN từ credential Git trả 401, truy cập anonymous trả 404 cho hai project private; Bearer ở lần trước trả 403. Không có pipeline ID/status/log để báo PASS. Git fetch/push FE hoạt động; đây là giới hạn quyền đọc API, không phải bằng chứng CI fail. Tiếp tục kiểm SHA bằng Git.

### Kết quả push đợt 13/10 đã xác minh

| Phần | GitLab repository | Branch | Commit source đã push | MR | Xác minh |
|---|---|---|---|---|---|
| FE | https://gitlab.com/maiiilinhhh220204-group/quanlysanbong/fe | codex/full-fix-20261012 | 3c388a4c9fb590b953f00c5e2bdd9f93f9afa3a2 | [FE !5](https://gitlab.com/maiiilinhhh220204-group/quanlysanbong/fe/-/merge_requests/5) | ls-remote khớp HEAD, tree sạch |
| BE | https://gitlab.com/maiiilinhhh220204-group/quanlysanbong/be | update-backend-0926 | 76843cc7aeb5c0a887273c1ea9ceccf493797179 | [BE !5](https://gitlab.com/maiiilinhhh220204-group/quanlysanbong/be/-/merge_requests/5) | ls-remote khớp HEAD, gitlink FE=3c388a4 |

Hai lần push thành công, không force và không ghi đè remote. Commit theo đúng message yêu cầu. MR title/description cập nhật theo scope đợt mới; không merge và không deploy. Backend có commit tài liệu tiếp theo để ghi kết quả cuối này; SHA cuối của tài liệu xem Git history, tránh SHA tự tham chiếu. Git status BE còn đúng root package-lock.json có sẵn; FE sạch.

**CI/CD: BLOCKED về xác minh trạng thái**, không có quyền/API credential phù hợp để đọc project/pipeline private; không ghi CI PASS. **Toàn hệ thống: chưa tuyên bố nghiệm thu đầy đủ** vì các giới hạn PostgreSQL/MinIO/export/production deployment/full E2E/assistive-technology audit và VNPay WAITING FOR INSTRUCTOR đã ghi ở trên. Phần code, kiểm thử cô lập và hai push trong đợt này đã thực hiện; các mục chưa có môi trường thật vẫn cần nghiệm thu tiếp.


## 2026-10-13 — Local JWT startup và tìm kiếm bên dưới Hero

- Backend: thêm profile `dev` đọc `.env.local` dưới dạng properties; `Initialize-Local.ps1` sinh JWT 48 byte ngẫu nhiên, không ghi đè key có sẵn. Không hard-code secret hoặc thay đổi `JwtTokenProvider`/bảo mật. Placeholder JWT rỗng được xử lý bởi kiểm tra fail-closed hiện có, thay cho lỗi placeholder thiếu.
- Thêm profile `smoke` nhập hai file bị Git bỏ qua, dùng H2 RAM và loopback để kiểm tra khi chưa có cấu hình dịch vụ local đầy đủ. README mô tả cách chạy dev/IntelliJ, biến môi trường và smoke.
- Đã chạy JAR thật profile smoke: Tomcat 8084, health 200/UP, danh sách sân 200, đăng ký 201, đăng nhập cấp JWT, API yêu thích xác thực 200. H2 cô lập; không tuyên bố đã kiểm tra PostgreSQL, MinIO hoặc giao dịch thật. Logic VNPay không sửa và không gọi cổng thanh toán.
- FE: `PremiumHomePage.jsx`, `Homepage.css` đưa toàn bộ form tìm kiếm ra ngoài Hero vào phần nền trắng, xóa tiêu đề/đoạn giới thiệu sân nổi bật trên trang chủ, chuyển sort ngay dưới form căn phải. Giữ nguyên ảnh, h1, mô tả, CTA và chiều cao Hero. Khoảng ảnh được giữ lại theo kích thước form cũ nên không thu nhỏ Banner.
- Đo trình duyệt trước/sau: viewport 375 / 520 / 768 / 1024 / 1440 / 1920 px; chiều cao Hero tương ứng 790.025 / 772.425 / 615.900 / 587.125 / 636.550 / 673.725 px, bằng nhau trước/sau; không tràn ngang. Search ngoài Hero, nền rgb(255,255,255), sort nằm dưới form.
- QA trình duyệt gọi Backend H2 fixture trên 8085: tên “kiểm thử 2” trả 1 sân; loại Sân 5 trả 2 sân; sort giảm giá trả sân 3 rồi 1; khu vực đúng trả 4 sân. Khách bấm yêu thích chuyển đăng nhập; tài khoản QA đăng nhập lưu sân 1 và thấy trong trang yêu thích.
- Kiểm thử: `mvn -o clean verify` với cache local: 35 test pass, JAR build thành công. FE `npm run lint` không warning, `npm test` 19 pass, `npm run build` thành công. BE không có task Java lint độc lập.
- Rà soát Git: đúng remote origin GitLab, BE `update-backend-0926`, FE `codex/full-fix-20261012`; fetch xác nhận cả hai 0 ahead / 0 behind trước commit. `.env.local`, `.env.smoke`, logs/build bị ignore. `package-lock.json` root là thay đổi có trước, không liên quan Maven, giữ local và không commit.
- Push đã thành công, `git ls-remote` khớp SHA local: FE `c4d4dd75e375d48229e6e3d7691c4d2f777d6607`, BE implementation `750c778167fbb12ab176d8d68c453557b4fed82f`. Báo cáo push được cập nhật bằng commit docs kế tiếp; SHA cuối xem lịch sử branch. Cả hai cập nhật MR !5 hiện có, không force push. Kiểm tra API pipelines theo SHA của cả FE/BE nhận HTTP 403 với credential hiện có: chưa xác minh được CI/CD remote. Không dùng build local để tuyên bố CI remote pass.
- Script local còn được kiểm tra với JWT rỗng: tự sinh key, giữ cấu hình DB đã có và giữ nguyên secret ở lần chạy sau. Rà soát staged diff không chứa secret vừa sinh, file môi trường hay private key/token pattern. Bỏ yêu thích và Hero CTA được xác minh trên trình duyệt.


## 2026-10-13 — Chẩn đoán chính xác lỗi JWT trong IntelliJ

- Cấu hình kiểm tra trước sửa: Run Configuration `BeCicdApplication` trong `.idea/workspace.xml` không có active profile, env JWT hoặc env file. `JWT_SECRET`, `JWT_EXPIRATION_MS`, `SPRING_PROFILES_ACTIVE` đều không có ở Process/User/Machine. Vì profile dev/smoke không bật, `.env.local` không được nhập: JWT secret hiệu lực rỗng (0 byte), expiration mặc định 86400000 ms hợp lệ. Key đã sinh trong `.env.local` có 64 byte UTF-8, không phải key ngắn.
- `JwtTokenProvider` tách lỗi secret thiếu/rỗng, secret dưới 32 byte và expiration không dương. Vẫn fail-closed, không thay đổi cơ chế ký JWT, không in giá trị secret và không tạo default secret.
- Sửa Run Configuration local: profile smoke, working directory `$PROJECT_DIR$`, environment `SPRING_PROFILES_ACTIVE=smoke`, `PORT=8084`, `JWT_EXPIRATION_MS=86400000`. Thêm cấu hình lưu riêng `BeCicdApplication Local Smoke` trong `.idea/runConfigurations/BeCicdApplication_Local_Smoke.xml`; không chép secret vào cấu hình IDE. Toàn bộ `.idea`, `.env.local`, `.env.smoke` đều bị Git ignore. Nếu IDE còn giữ cấu hình cũ trong bộ nhớ, chọn cấu hình Local Smoke đã lưu.
- Khởi động JAR mới bằng environment lấy trực tiếp từ file Run Configuration: profile smoke, Tomcat 8084, health 200/UP, danh sách sân 200, đăng ký 201, login 200, API yêu thích có JWT 200, không JWT 401. Kiểm tra HMAC chứng minh token runtime dùng đúng key local; exp-iat=86400 giây.
- `mvn -o clean verify`: 40 test pass, 0 failure/error/skip, build/repackage thành công. Thêm regression test cho secret thiếu, secret ngắn không bị lộ trong message, expiration 0/-1 và độ dài key tính theo byte UTF-8. Lần clean đầu bị JAR cũ giữ trên Windows; đã dừng tiến trình local cũ và chạy lại thành công.
- Giới hạn: ứng dụng được chạy bằng JAR với cùng cấu hình đã lưu, chưa xác minh bằng nút Run trực tiếp trong UI IntelliJ. Profile smoke dùng H2 RAM cô lập; chưa kiểm tra PostgreSQL/MinIO/thanh toán thật. Không sửa VNPay, FE hoặc nghiệp vụ khác. Secret local giữ nguyên, không commit vào GitLab.
