# Báo cáo kiểm tra Quản Lý Sân Bóng — 12/10/2026

Phạm vi: mã nguồn Frontend, Backend, Entity/Repository/DTO, cấu hình build, Docker và CI/CD trong workspace hiện tại. Các đường dẫn ngắn bên dưới tính từ `D:/Năm 3/RaiWay99/QuanLiSanBong`. Frontend là Git repository riêng nằm trong `fe`; trạng thái repository cha đã có thay đổi `fe` và `package-lock.json` trước khi kiểm tra.

**Giới hạn bằng chứng:** đã biên dịch hai phía và kiểm tra render header. Chưa kiểm thử HTTP nghiệp vụ, JWT thực tế, thanh toán, MinIO hoặc truy vấn schema PostgreSQL. Không khởi động Backend với cấu hình hiện tại vì `ddl-auto: update` và ba initializer có thể ghi vào PostgreSQL từ xa. Không sửa Backend, cấu hình Database hoặc dữ liệu. Những lỗi nghiệp vụ dưới đây là kết luận từ luồng code, chưa phải kết quả tái hiện trên Database thật.

## 1. Tổng quan cấu trúc và công nghệ

- Frontend: React 18, Vite 4, TailwindCSS 3, Axios và Fetch; điều hướng bằng `window.history` trong `fe/src/App.jsx`, không dùng React Router trong luồng hiện hành.
- Backend: Spring Boot 2.7.0, Java target 11, Spring MVC, Spring Security, BCrypt, JJWT, Spring Data JPA/Hibernate; Maven. Có 76 file Java, 18 controller, 9 Entity và 9 Repository.
- Database cấu hình: PostgreSQL từ xa; có dependency H2 và bật H2 console nhưng datasource mặc định không phải H2. Không tìm thấy migration, SQL schema hay mã test trong source.
- Dịch vụ: MinIO lưu ảnh/báo cáo, VNPay sandbox; Apache POI cho Excel, iText cho PDF.
- Triển khai: Docker nhiều stage; Frontend chạy Nginx với SPA fallback; hai pipeline GitLab riêng. FE test stage đang comment, BE Docker build bỏ test.
- Vai trò thực tế: `KHACH_HANG`, `NHAN_VIEN_PAGE`, `NHAN_VIEN_SAN`, `ADMIN`, cùng trạng thái khách chưa đăng nhập trên FE.
- Luồng thông thường: Page → API module → Fetch/Axios → Controller → Repository → JPA → PostgreSQL → DTO/Map → state. Nhiều controller gọi thẳng Repository, không có Service nghiệp vụ ở giữa. VNPay/Product dùng Service; upload dùng MinioService. Đây là mô tả kiến trúc hiện có, không phải yêu cầu refactor.

## 2. Tổng số chức năng phát hiện

**39 hạng mục kiểm tra**, được định nghĩa ở bảng dưới; đây là cách nhóm nghiệp vụ để thống kê, không phải số component hay số endpoint. Tìm được **67 handler HTTP, tương ứng 72 cặp method/URL** khi mở rộng các alias trong annotation. Không tính Swagger, H2 console hoặc endpoint framework tự tạo vào 72 API ứng dụng.

Mỗi hàng có một trạng thái chính, ưu tiên `CÓ LỖI` khi đã có lỗi cụ thể; chức năng có lỗi vẫn có thể đã gọi API trong code. Cột Database chỉ nói có mapping/lưu trữ trong source, không xác nhận bảng thực tế.

| STT | Chức năng | Frontend | Backend API | Database | Trạng thái | Vấn đề/bằng chứng |
|---|---|---|---|---|---|---|
| 1 | Đăng nhập | Có, LoginPage | POST auth/dang-nhap | NguoiDung | ĐÃ KẾT NỐI API | Body username/password, response token/role tương thích; loading/error và lưu phiên có code; chưa chạy HTTP |
| 2 | Đăng xuất | Có, App.handleLogout | Không cần trong thiết kế hiện tại | Không ghi DB | CHƯA XÁC MINH | Xóa 3 khóa localStorage và điều hướng; JWT cũ còn hiệu lực đến hết hạn; chưa kiểm thử tương tác |
| 3 | Đăng ký | Có, RegisterPage | POST auth/dang-ky | NguoiDung | CÓ LỖI | Backend không bắt buộc hoTen dù cột NOT NULL; email dùng làm username giới hạn 50 ký tự; F13 |
| 4 | Xác thực và phân quyền | App guards + token | SecurityConfig/JwtAuthFilter | NguoiDung.role/khóa | CÓ LỖI | Token cũ của tài khoản bị khóa vẫn được xác thực; F01/F02/F16 |
| 5 | Xem/sửa hồ sơ | Có, ProfilePage | PUT me; GET me chưa dùng | NguoiDung | CÓ LỖI | Không nạp hồ sơ thật; login không trả SĐT nên hồ sơ có thể rỗng và ghi đè; F12 |
| 6 | Đổi mật khẩu | Có | PUT doi-mat-khau | NguoiDung.password | ĐÃ KẾT NỐI API | Khớp matKhauCu/matKhauMoi, kiểm BCrypt, loading/success/error; thiếu validation null cũ; F13 |
| 7 | Quên mật khẩu | Trang hướng dẫn liên hệ admin | Không có endpoint | Không | CHƯA HOÀN THIỆN | Wrapper quenMatKhau gọi URL không tồn tại; trang hiện tại không gọi wrapper; F20 |
| 8 | Danh sách sân công khai | HomePage | GET san | San/GiaSan/CumSan | CÓ LỖI | Không lọc sân tạm ngưng/ẩn; giá và nhãn trống giả định; F06/F17 |
| 9 | Tìm/lọc/sắp xếp sân | Có, HomePage | Dùng GET san | Dữ liệu sân qua API | ĐÃ KẾT NỐI API | Lọc client theo tên/loại/cụm, sắp xếp giá; không có API tìm riêng; chưa chạy với dữ liệu thực |
| 10 | Lịch trống theo ngày | FieldDetailPage | GET san/{id}/lich-trong | GiaSan/DatSan | CÓ LỖI | Giữ chỗ hết hạn vẫn bị coi đã đặt; retry không đổi state; F07/F14 |
| 11 | Đặt sân online trả tại sân | FieldDetailPage | POST dat-san | DatSan + ThanhToan | CÓ LỖI | Có transaction và snapshot giá nhưng không chặn ngày/giờ đã qua, sân tạm ngưng; F05/F06 |
| 12 | Chống trùng lịch | FE dựa lịch; BE check | POST dat-san và walkin | DatSan | CÓ LỖI | Check-then-insert không khóa/constraint; chỉ so ID khung giờ, bỏ giao thời gian; F04 |
| 13 | Hủy đơn | BookingHistoryPage | PUT dat-san/{id}/huy | DatSan | CÓ LỖI | Điều kiện thời gian sai cho ngày quá khứ; không đồng bộ thanh toán/hoàn tiền; F08 |
| 14 | Lịch sử đặt sân | BookingHistoryPage | GET dat-san/cua-toi | DatSan | CÓ LỖI | Có tải lại sau hủy nhưng error cũ không được xóa khi retry; F14 |
| 15 | Sân yêu thích | HomePage/FavoriteFieldsPage | GET/POST/DELETE yeu-thich | YeuThich, unique user/sân | ĐÃ KẾT NỐI API | Ghi DB trước cập nhật state; GET ban đầu HomePage nuốt lỗi; chưa kiểm thử cạnh tranh |
| 16 | CRUD cụm sân | AdminCumSanPage | admin/cum-san | CumSan | ĐÃ KẾT NỐI API | Body/response khớp; xóa chặn cụm có sân; cần kiểm FK nhân viên còn tham chiếu; F11 |
| 17 | Phân công nhân viên theo cụm | AdminCumSanPage | PUT cum-san/nhan-vien/{id} | NguoiDung.cumSan | CHƯA HOÀN THIỆN | Có lưu phân công nhưng các API staff vẫn lấy toàn bộ; cần xác nhận ý nghĩa phân công; F10 |
| 18 | CRUD/trạng thái sân | AdminQuanLySanPage | admin/san CRUD/trang-thai | San | CÓ LỖI | PUT bỏ qua cumSanId; DELETE cascade có thể xóa lịch sử; F09/F11 |
| 19 | Loại sân | Select trong quản lý sân | Trường loaiSan | San.loaiSan | CHƯA HOÀN THIỆN | Không có danh mục loại sân riêng; seeder dùng 5 NGUOI/7 NGUOI khác Sân 5/Sân 7; F15 |
| 20 | Quản lý khung giờ | Tạo/xóa trong AdminSchedulePage | GET/POST/PUT/DELETE khung-gio | KhungGio | CÓ LỖI | Xóa cascade đơn/giá/thanh toán; chưa chống giờ trùng/giao; PUT chưa có UI; F04/F11 |
| 21 | Giá theo sân/giờ/loại ngày | AdminSchedulePage | gia-san CRUD | GiaSan | ĐÃ KẾT NỐI API | Payload sanId/khungGioId/gia/loaiNgay khớp; validation giá >0 và kiểm trùng; unique nullable cần kiểm schema |
| 22 | Duyệt/từ chối yêu cầu | BookingRequestsPage | nhan-vien/dat-san, duyet, tu-choi | DatSan | ĐÃ KẾT NỐI API | Lọc CHO_XAC_NHAN, body lyDo, tải lại sau mutation; giao dịch đơn bị từ chối vẫn chờ, F08 |
| 23 | Ma trận lịch nhân viên | StaffScheduleMatrix | GET nhan-vien/lich-ma-tran | San/GiaSan/DatSan | ĐÃ KẾT NỐI API | DTO slots khớp; loại bỏ giữ chỗ hết hạn; không polling; giá trong ô đơn là giá hiện tại, F18 |
| 24 | Tìm đơn và check-in POS | PosCheckinPage | tim-kiem, checkin | DatSan | CÓ LỖI | Backend chỉ kiểm trạng thái, không kiểm ngày/giờ check-in; F19 |
| 25 | Đặt tại quầy | WalkinBookingPage | POST nhan-vien/dat-san/walkin | NguoiDung/DatSan/ThanhToan | CÓ LỖI | Thiếu tongTien bắt buộc; lựa chọn VNPay không tạo link; F03/F21 |
| 26 | Quản lý/xác nhận thanh toán | AdminPaymentsPage | thanh-toan/quan-ly, xac-nhan | ThanhToan | CÓ LỖI | Có thể xác nhận thu cho đơn đã hủy; chưa có trang cho staff onsite dù API cho phép; F08/F22 |
| 27 | Hóa đơn đơn đặt sân | BookingHistoryPage modal | GET thanh-toan/dat-san/{id}/hoa-don | DatSan/ThanhToan | ĐÃ KẾT NỐI API | DTO Map khớp, kiểm chủ đơn hoặc staff; hóa đơn tổng hợp, không có Entity hóa đơn độc lập |
| 28 | VNPay tạo link/nhận kết quả | FieldDetailPage/lịch sử | POST create; GET return | ThanhToan + DatSan | CÓ LỖI | Thiếu tongTien, không kiểm chủ đơn, callback cần JWT, thiếu transaction/idempotency; F03/F16/F23 |
| 29 | Dashboard | AdminDashboardPage | GET admin/dashboard | Người dùng/đơn/giao dịch | CÓ LỖI | Lỗi được lưu stats.error nhưng không render; không có loading; F14 |
| 30 | Thống kê doanh thu | AdminDoanhThuPage | GET admin/doanh-thu | ThanhToan.THANH_CONG | CÓ LỖI | Nút Tuần lệch ngày theo UTC; label tăng trưởng/số lượt đặt dùng sai chỉ số; F17 |
| 31 | Xuất Excel doanh thu | AdminBaoCaoPage | GET bao-cao/excel | ThanhToan → POI → MinIO | ĐÃ KẾT NỐI API | Trả JSON url khớp FE, không phải binary response; truy cập MinIO cần xác minh F24 |
| 32 | Xuất PDF doanh thu | AdminBaoCaoPage | GET bao-cao/pdf | ThanhToan → iText → MinIO | ĐÃ KẾT NỐI API | Trả JSON url khớp; chưa xác minh nội dung, font và quyền truy cập file; F24 |
| 33 | Báo cáo tần suất/khách thân thiết | Có option select | Không có API tương ứng | Không có truy vấn tương ứng | CHƯA KẾT NỐI API | Select không có state/onChange, luôn xuất doanh thu; F17 |
| 34 | Quản lý sản phẩm demo | ProductForm/API rời | products CRUD/image | Product | CHƯA HOÀN THIỆN | Không route/import vào App; wrapper thiếu token; objectName nhận URL; F20/F24 |
| 35 | CRUD/khóa nhân viên | AdminNhanVienPage | admin/nhan-vien, nguoi-dung/khoa | NguoiDung | CÓ LỖI | Tải danh sách nuốt lỗi; sửa vai trò thiếu giới hạn, email thiếu kiểm trùng; F02/F13/F14 |
| 36 | CRUD/khóa khách hàng | AdminKhachHangPage | admin/khach-hang, nguoi-dung/khoa | NguoiDung | CÓ LỖI | Tải nuốt lỗi; xóa người có đơn vướng FK; khóa không chặn JWT cũ; F02/F11/F13 |
| 37 | Avatar cá nhân | FileReader + localStorage | Không có API avatar | Không có cột avatar | CHƯA KẾT NỐI API | Chỉ state/localStorage, không lưu DB; F12 |
| 38 | Màn hình staff cũ đặt hộ/POS | StaffPages.jsx không được import | Các URL cũ không tồn tại | Không có luồng hiện hành | CHƯA HOÀN THIỆN | Ba lời gọi API sai, code không vào routing; F20 |
| 39 | Health check | Wrapper productApi.health không dùng | GET api/health | Không kiểm DB | CHƯA XÁC MINH | Trả UP cố định; không có bằng chứng HTTP hoặc trạng thái dependency |

Thay đổi lịch đặt sân chưa tìm thấy page/endpoint. Chuyển đơn sang `HOAN_THANH` chỉ có enum và code đọc, không tìm thấy luồng ghi trạng thái này. Không tự kết luận hai nghiệp vụ đó thuộc yêu cầu đã xác nhận và không thêm code. Quản lý loại sân độc lập cũng chưa có; hiện chỉ lưu chuỗi trong San.

## 3. Số chức năng hoàn thành có kiểm thử xác nhận

**0/39 chức năng nghiệp vụ được công nhận HOÀN THÀNH**. Build pass không chứng minh đăng nhập, đặt sân, thanh toán hay lưu Database thành công. Thay đổi header đã được kiểm tra render riêng, không được tính là chứng nhận nghiệp vụ.

## 4. Số chức năng đã kết nối API nhưng chưa kiểm thử

**11/39** có trạng thái chính ĐÃ KẾT NỐI API: 1, 6, 9, 15, 16, 21, 22, 23, 27, 31, 32. Các hàng CÓ LỖI/CHƯA HOÀN THIỆN có lời gọi API vẫn giữ dấu “Có” ở cột API; không cộng lại vào 11 để tránh đếm trùng.

## 5. Số chức năng chưa kết nối API

**2/39**: báo cáo tần suất/khách thân thiết và avatar. Đăng xuất thiết kế hiện tại là thao tác local nên không bị kết luận thiếu API. Quên mật khẩu chỉ hướng dẫn liên hệ admin, được xếp chưa hoàn thiện.

## 6. Số chức năng chưa hoàn thiện hoặc có lỗi

**24/39**, gồm **19 CÓ LỖI** và **5 CHƯA HOÀN THIỆN**. Hai hàng còn lại CHƯA XÁC MINH là đăng xuất và health. Tổng kiểm tra: 0 + 11 + 2 + 24 + 2 = 39.

## 7. Kiểm kê API và API chưa được Frontend sử dụng

Mọi hàng dưới đây mới được đối chiếu method, URL, payload và nơi gọi trong source; **chưa endpoint nào được kiểm thử HTTP thành công trong lần kiểm tra này**. FE dùng `http` trả `{data,status,ok}` hoặc Axios staff trả `{data,...}`. Các wrapper auth/san/datSan/favorite có hàm tháo `.data`; đã đối chiếu tương ứng với cách Page sử dụng.

| Controller / toàn bộ nhóm method-URL | FE hiện hành / hợp đồng dữ liệu | Quyền và lưu trữ / kết luận |
|---|---|---|
| AuthController: POST `/api/auth/dang-nhap`, `/dang-ky` | LoginPage gửi username/password; RegisterPage gửi email/password/hoTen/soDienThoai | Công khai; BCrypt/token hoặc save NguoiDung; F13 |
| NguoiDungController: GET/PUT `/api/nguoi-dung/me`; PUT `/doi-mat-khau` | PUT me gửi hoTen/soDienThoai/email; đổi MK gửi matKhauCu/matKhauMoi; GET me wrapper không dùng | Có JWT; dựa user hiện tại; GET chưa vào ProfilePage |
| SanPublicController: GET `/api/san`, `/api/san/{sanId}/lich-trong?ngay=` | HomePage lấy SanDTO[]; FieldDetailPage lấy LichTrongDTO[] có khungGioId/time/gia/trangThai | Công khai; response là DTO, không phải Entity San thô như comment cũ trong sanApi; F06/F07 |
| SanController: GET `/api/nhan-vien/san` | Không dùng trong App hiện hành | Staff/admin theo SecurityConfig; trả toàn bộ SanDTO; không lọc cụm |
| SanAdminController: POST `/api/admin/san`; PUT/DELETE `/{id}`; PUT `/{id}/trang-thai?trangThai=`; POST `/upload-anh` | AdminQuanLySanPage; JSON tạo/sửa, multipart field file upload; JSON url upload | Admin theo matcher URL; ghi San/MinIO; PUT bỏ cumSanId F09 |
| KhungGioController: GET/POST `/api/khung-gio`; GET/PUT/DELETE `/{id}` | AdminSchedulePage dùng GET list, POST, DELETE; GET detail không có wrapper; PUT có wrapper chưa gọi | GET cần JWT theo fallback security; mutation admin; giờ LocalTime; F04/F11 |
| GiaSanController: GET/POST `/api/gia-san`; PUT/DELETE `/{id}` | AdminSchedulePage; GET optional sanId, body sanId/khungGioId/gia/loaiNgay; GiaSanDTO[] | GET cần JWT; mutation admin; save/delete GiaSan |
| CumSanAdminController: GET/POST `/api/admin/cum-san`; PUT/DELETE `/{id}`; PUT `/nhan-vien/{staffId}` | AdminCumSanPage và danh mục trong quản lý sân; body tenCum/diaChi/moTa hoặc cumSanId/null | Admin; Map JSON; lưu cụm/phân công nhưng chưa thực thi scope F10 |
| AdminUserController: GET/POST `/api/admin/khach-hang`, `/nhan-vien`; PUT/DELETE `/khach-hang/{id}`, `/nhan-vien/{id}`; PUT `/nguoi-dung/{id}/khoa` | Hai page Admin; JSON hoTen/sdt/email/password khi tạo, vaiTro cho staff; Map an toàn không password | Admin; NguoiDung; duplicate email/username kiểm lúc tạo, thiếu khi sửa; F02/F13 |
| DatSanController: POST `/api/dat-san`; GET `/cua-toi`, `/giu-cho/cua-toi`, `/{id}`; PUT `/{id}/huy` | FieldDetailPage/lịch sử dùng POST, GET cua-toi, PUT huy; body sanId/khungGioId/ngayDat/phuongThucThanhToan | KHACH_HANG; kiểm sở hữu GET detail/hủy; GET giữ chỗ/detail wrapper chưa dùng; F04–F08 |
| NhanVienDatSanController: GET `/api/nhan-vien/dat-san?trangThai=`; PUT `/{id}/duyet`, `/{id}/tu-choi`; GET `/tim-kiem?q=`; PUT `/{id}/checkin`; POST `/walkin` | StaffPagePages và StaffOnsitePages qua staffApi; lyDo khi từ chối; walkin thêm tên/SĐT/phương thức | Duyệt/từ chối: ADMIN/PAGE; tìm/checkin/walkin: ADMIN/SAN; JSON DatSanDTO; F03/F04/F19/F21 |
| QuanLySanController: GET cả 4 URL `/api/quan-ly/ma-tran-lich`, `/api/quan-ly/lich-ma-tran`, `/api/nhan-vien/ma-tran-lich`, `/api/nhan-vien/lich-ma-tran`, query ngay | App hiện hành chỉ gọi `/api/nhan-vien/lich-ma-tran`; StaffPages cũ gọi quan-ly/ma-tran-lich nhưng không import | Hai staff/admin; MatrixLichSanDTO[] với slots; lọc giữ chỗ hết hạn; F10/F18 |
| ThanhToanController: GET `/api/thanh-toan/quan-ly?trangThai=`; GET `/dat-san/{datSanId}`, `/dat-san/{datSanId}/hoa-don`; PUT `/{id}/xac-nhan` | AdminPaymentsPage dùng list/confirm; BookingHistoryPage dùng hóa đơn; GET giao dịch theo đơn chưa dùng | List/confirm ADMIN/SAN; xem theo đơn JWT và sở hữu hoặc ADMIN/SAN; Map DTO; F08/F22 |
| VNPayController: POST `/api/thanh-toan/vnpay/create/{datSanId}`; GET `/return` | authApi.taoLinkVNPay → `{paymentUrl}` → redirect; callback dành cho trình duyệt/cổng thanh toán | Fallback yêu cầu JWT cho cả return; create thiếu kiểm sở hữu; Service thiếu tongTien và transaction; F03/F16/F23 |
| YeuThichController: GET `/api/yeu-thich`; POST/DELETE `/{sanId}` | HomePage + FavoriteFieldsPage qua favoriteApi; không body; SanDTO[]/SanDTO/204 | KHACH_HANG; lấy user từ principal; mutation transaction; unique DB; thêm đồng thời có thể conflict |
| AdminDashboardController: GET `/api/admin/dashboard`; GET `/doanh-thu`, `/bao-cao/excel`, `/bao-cao/pdf` với tuNgay/denNgay | AdminPages; stats Map, doanh thu Map; export trả JSON url, không yêu cầu responseType blob | Admin; doanh thu từ thanh toán thành công theo thời điểm thanh toán; POI/iText + MinIO; F14/F17/F24 |
| ProductController: GET/POST `/api/products`; GET/PUT/DELETE `/{id}`; GET/PUT `/{id}/image` | Không vào App; productApi có list/create/update/remove, không token; ProductForm không có parent đang dùng | GET cần JWT; tạo/sửa staff/admin, xóa admin; tạo/đổi ảnh multipart, sửa metadata JSON; F20/F24 |
| HealthController: GET `/api/health` | productApi.health tồn tại nhưng không caller hiện hành | Công khai, trả UP/time, không truy vấn DB; chưa chạy HTTP |

**Không được gọi từ giao diện hiện hành** (không đồng nghĩa cần phải gọi):

1. GET `/api/nguoi-dung/me` — có authApi.getProfile, chưa caller.
2. GET `/api/nhan-vien/san` — không caller.
3. GET `/api/khung-gio/{id}` — không caller.
4. PUT `/api/khung-gio/{id}` — có adminApi.suaKhungGio, chưa UI sửa.
5. GET `/api/dat-san/giu-cho/cua-toi` và GET `/api/dat-san/{id}` — wrappers chưa caller.
6. GET `/api/thanh-toan/dat-san/{datSanId}` — không caller; hóa đơn hiện hành dùng endpoint khác.
7. Ba alias matrix `/api/quan-ly/ma-tran-lich`, `/api/quan-ly/lich-ma-tran`, `/api/nhan-vien/ma-tran-lich` — chỉ alias thứ nhất có trong module staff cũ không được import.
8. Toàn bộ 7 endpoint products — module rời, không luồng App.
9. GET `/api/health` — wrapper rời, không UI caller.

Tổng **18 cặp method/URL không được gọi từ FE hiện hành**, gồm alias và API demo. GET VNPay return không được tính là API bỏ quên vì là callback, nhưng chưa được kiểm thử hoạt động.

**Lời gọi FE không có endpoint:** authApi.quenMatKhau → POST `/api/auth/quen-mat-khau`; StaffPages.MatrixCalendarPage → POST `/api/quan-ly/giu-cho-tam`; StaffPages.OnsitePosPage → GET `/api/quan-ly/tim-don?query=...`, PUT `/api/quan-ly/xac-nhan-vao-san/{id}`. Tất cả đều không thuộc routing hiện hành.

**Cấu hình URL/CORS:** `fe/.env` hiện đặt localhost:8084 nên override fallback; `http.js` fallback IP LAN, `staffApi.js`/productApi/QuanLySan fallback localhost. Docker build nhận VITE_API_BASE_URL, GitLab FE đặt địa chỉ VPS. Vì localhost được trình duyệt của người dùng giải nghĩa, bundle triển khai sai URL sẽ gọi máy người dùng. Không tìm thấy proxy API trong Vite/Nginx. SecurityConfig cho phép origin pattern * và credentials; CorsConfig lại chỉ hai origin cụ thể; một số controller còn CrossOrigin *. Chưa đo preflight thực tế để kết luận CORS đang lỗi. HTTPS FE gọi các default HTTP có nguy cơ mixed content.

## 8. Mock, hard-code và dữ liệu chỉ lưu local

| Vị trí | Dữ liệu | Có trong luồng hiện hành? |
|---|---|---|
| `fe/src/data/mock.js` | FIELDS, TIMESLOTS, SLOT_STATUS, BOOKINGS, CUSTOMERS, STAFF_LIST, REVENUE_DATA, CLUSTER_REVENUE, TRANSACTIONS | Không tìm thấy import trong source; không kết luận các page hiện hành đang dùng các mảng này |
| `fe/src/pages/GuestPages.jsx`, HomePage | Nhãn “Còn trống” trên mọi card; fallback giá 300.000 đ, ảnh mẫu và mô tả mẫu | Có; giá/trạng thái này không có bằng chứng từ lịch API |
| HomePage | 24/7, 100%, VNPay và banner marketing | Có; là nội dung tĩnh, không phải chỉ số thống kê Database |
| `fe/src/pages/AdminPages.jsx`, AdminBaoCaoPage | Nhãn báo cáo ngày 25/09/2026, tuần 21–27/09/2026, tháng 09/2026 | Có; nút đều gửi khoảng ngày form hiện tại, không dùng khoảng ngày trong nhãn |
| `fe/src/components/Layout.jsx` | Đổi hiển thị riêng NgoQuangManh → Ngô Quang Mạnh | Có; ánh xạ hard-code tên |
| `fe/src/pages/GuestPages.jsx`, ProfilePage | Avatar dạng Data URL trong localStorage | Có; không persist trên Database |
| `config/SanBongDataSeeder.java` | Tài khoản/sân/giá demo khi DB rỗng | Chạy startup không giới hạn profile; dữ liệu mẫu được ghi DB thật theo datasource |
| `config/SanDataInitializer.java`, `ScheduleDataInitializer.java` | Tự tạo đủ 5 sân mỗi loại và khung giờ/giá cố định | Chạy startup kể cả DB có dữ liệu; không phải FE mock |

State FE sau mutation đã thấy: thêm/sửa/xóa sân/người dùng/cụm/giá và duyệt/từ chối đều gọi API trước rồi reload; yêu thích gọi API trước rồi thay state; check-in tìm lại đơn; walkin tăng reloadKey. Các vấn đề riêng: avatar local, cumSanId gửi nhưng không lưu, retry lịch không gọi lại API, lỗi lịch sử không reset và dashboard giấu error.

## 9. Các vấn đề theo mức độ ưu tiên

Các mã F dùng lại trong bảng chức năng. Mỗi mục nêu file, hàm, nguyên nhân và đề xuất; **chỉ đề xuất, chưa triển khai sửa**.

| Mã | Ưu tiên | File / hàm / endpoint | Nguyên nhân và ảnh hưởng | Khắc phục đề xuất |
|---|---|---|---|---|
| F01 | Critical | `src/main/resources/application.yml`; `security/JwtTokenProvider.java#getSigningKey` | Thông tin xác thực DB/MinIO/VNPay nằm trực tiếp trong cấu hình; JWT có secret fallback cố định trong source. Nếu triển khai dùng fallback và source bị lộ, có thể giả chữ ký token cho username đang tồn tại | Chuyển secrets sang môi trường/secret manager; thay các khóa đã chia sẻ; JWT thiếu secret phải fail startup; không sao chép giá trị secret vào báo cáo |
| F02 | High | `security/JwtAuthFilter.java#doFilterInternal`; `CustomUserDetails#isEnabled`; `controller/AdminUserController#toggleKhoaAccount` | Filter tạo Authentication trực tiếp sau load user, không gọi kiểm isAccountNonLocked/isEnabled. Khóa chỉ chặn login mới, token cũ còn vào API | Kiểm trạng thái user trước setAuthentication; trả 401/403 thống nhất; test JWT trước/sau khóa |
| F03 | High | `service/VNPayService.java#createPaymentUrl`; `controller/NhanVienDatSanController.java#taoDonTaiQuay`; `entity/ThanhToan.java#tongTien` | Cả hai nhánh tạo ThanhToan mới gọi setSoTien nhưng không setTongTien, trong khi mapping nullable=false. VNPay có thể đã lưu booking trước khi thất bại; walkin có transaction nên rollback | Gán tổng tiền đúng nghiệp vụ cho mọi nhánh; kiểm DB schema/constraint; thêm kiểm thử transaction với DB test |
| F04 | High | `controller/DatSanController.java#taoDatSan`; `NhanVienDatSanController#taoDonTaiQuay`; `KhungGioController#kiemTraKhoangGio`; `repository/DatSanRepository.java`; `entity/DatSan.java` | Hai request đồng thời đều đọc slot trống rồi insert; @Transactional không tự serialize. Hai KhungGio khác ID nhưng giao giờ cũng không bị chặn; tạo giờ không kiểm trùng/giao | Chọn chiến lược lock/constraint phù hợp PostgreSQL; kiểm overlap theo thời gian thực; test cạnh tranh, không chỉ test tuần tự |
| F05 | High | `controller/DatSanController.java#taoDatSan`; `dto/TaoDatSanRequest.java`; `fe/src/pages/GuestPages.jsx#FieldDetailPage` | Request chỉ @NotNull ngày, Backend không chặn ngày/giờ đã qua; input ngày FE không min | BE kiểm LocalDateTime bắt đầu, ngày hợp lệ và phương thức không null; FE bổ sung giới hạn chỉ hỗ trợ UX |
| F06 | High | `controller/SanPublicController.java#danhSachSan/xemLichTrong`; `DatSanController#taoDatSan`; `NhanVienDatSanController#taoDonTaiQuay`; `QuanLySanController#layMaTranLich` | findAll hoặc findById không kiểm San.trangThai; đổi tạm ngưng/ẩn không chặn listing/booking/API direct | Xác định whitelist trạng thái, lọc công khai và chặn đặt sân ngừng hoạt động tại Backend |
| F07 | High | `controller/SanPublicController.java#xemLichTrong` so với `DatSanController#conHieuLuc`, `QuanLySanController#dangChiemCho` | Public giữ tất cả GIU_CHO_TAM, không kiểm deadline; FE khóa ô nhưng API đặt/ma trận staff đã coi trống | Dùng cùng quy tắc hiệu lực; xác định cách dọn trạng thái hết hạn và test thời gian |
| F08 | High | `controller/DatSanController.java#huyDatSan`; `NhanVienDatSanController#tuChoiDon`; `ThanhToanController#xacNhanThanhToan` | Hủy/từ chối chỉ đổi booking; payment vẫn chờ hoặc thành công. Confirm không kiểm booking hủy nên có thể ghi nhận thu cho đơn hủy. Điều kiện hủy dùng ngày <= hôm nay AND giờ <= hiện tại nên ngày cũ giờ lớn vẫn có thể hủy | Đồng bộ booking/payment trong transaction; xác định hoàn tiền cho giao dịch đã trả; so thời điểm ngày+giờ; chặn xác nhận đơn hủy |
| F09 | High | `fe/src/pages/AdminPages.jsx#AdminQuanLySanPage.handleSave`; `controller/SanAdminController.java#suaSan` | FE gửi cumSanId khi sửa nhưng BE chỉ đọc tenSan/loaiSan/hinhAnh/moTa; trả 200 trong khi cụm không đổi | Validate và áp dụng cumSanId, kể cả bỏ cụm/null; kiểm lưu rồi reload |
| F10 | Medium | `controller/CumSanAdminController.java#phanCongNhanVien`; `SanController#xemDanhSachSan`; `QuanLySanController#layMaTranLich`; các hàm NhanVienDatSanController | Lưu NguoiDung.cumSan nhưng danh sách/duyệt/tìm/checkin/walkin không sử dụng scope. Comment SanController nói chưa có quan hệ đã lỗi thời | Xác nhận phân công là thông tin hay giới hạn quyền; nếu là giới hạn quyền, kiểm tất cả read/write ở BE. Khi được xác nhận là scope bảo mật, ưu tiên nâng High |
| F11 | High | `entity/San.java`, `KhungGio.java`, `DatSan.java` (cascade ALL); `controller/SanAdminController#xoaSan`, `KhungGioController#xoa`, `AdminUserController#xoaNguoiDung`, `CumSanAdminController#xoa` | Xóa sân/khung giờ cascade tới booking rồi payments; favorites có thể khiến FK chặn xóa. Xóa user có booking/favorite và xóa cụm còn staff tham chiếu cũng có nguy cơ lỗi FK; không có handling riêng | Chặn hard delete đối tượng có lịch sử hoặc dùng trạng thái ngừng; kiểm tất cả quan hệ; trả 409 rõ ràng; xác minh với schema thật trước migration |
| F12 | Medium | `fe/src/pages/GuestPages.jsx#ProfilePage.handleCapNhatThongTin`; `api/authApi.js#getProfile`; `controller/AuthController#dangNhap`; `entity/NguoiDung.java` | Profile chỉ đọc localStorage, không GET me; login không trả số điện thoại. Save tên/email có thể gửi SĐT rỗng ghi đè giá trị thật. Avatar chỉ lưu local; header parent không có cơ chế cập nhật tên ngay | Load profile từ API; lưu theo response authoritative; nếu avatar thuộc phạm vi thì thiết kế persistence sau phê duyệt; cập nhật phiên/UI có sự kiện |
| F13 | Medium | `controller/AuthController.java#dangKy/dangNhap`; `AdminUserController#capNhatNguoiDung/taoNguoiDung`; `NguoiDungController#capNhatCaNhan/doiMatKhau`; `entity/NguoiDung.java` | Thiếu @Valid/giới hạn tên/email/SĐT; hoTen DB NOT NULL nhưng đăng ký API không bắt buộc; email làm username tối đa 50 trong khi email 100; sửa email thiếu check duplicate ở admin; sửa vaiTro nhận mọi enum không kiểm loại route; password cũ null chưa xử lý rõ | DTO validate theo cột, canonical email và username; whitelist vai trò theo nghiệp vụ, tránh hạ vai trò admin ngoài ý muốn; conflict trả 409; null trả 400 |
| F14 | Medium | `fe/src/pages/AdminPages.jsx#AdminDashboardPage/AdminKhachHangPage/AdminNhanVienPage`; `GuestPages#BookingHistoryPage.taiLichSu`, `FieldDetailPage` retry | Dashboard set stats.error nhưng không render; danh sách user catch rỗng và không loading, lỗi trông như không có dữ liệu; lịch sử không reset error; retry lịch set cùng ngayDat nên effect không chạy | Phân biệt loading/empty/error; retry bằng hàm tải hoặc reloadKey; clear error khi bắt đầu; hiển thị thông báo và chống submit lặp |
| F15 | Medium | `config/SanBongDataSeeder.java#run`; `SanDataInitializer#run`; `ScheduleDataInitializer#run`; `fe/src/pages/AdminPages.jsx` select loaiSan | Ba CommandLineRunner không profile/@Order, có thể tạo/thay dữ liệu trên DB thật mỗi startup. Seeder dùng loại sân khác UI; ScheduleDataInitializer chỉ hỗ trợ Sân 5/7/11; thứ tự có thể khiến giá chưa được tạo ngay | Tách seed theo môi trường; xác định thứ tự/idempotency; chuẩn hóa mã loại sân sau duyệt; không chạy initializer lên DB thật để thử |
| F16 | High | `config/SecurityConfig.java#filterChain`; `controller/VNPayController.java#taoLinkThanhToan/xuLyKetQua`; `service/VNPayService.java#createPaymentUrl`; `config/VNPayConfig.java`; `application.yml#return-url` | create không kiểm chủ đơn/trạng thái; mọi user có JWT có thể tạo link cho ID người khác. return chịu anyRequest.authenticated; browser redirect không tự gửi Bearer localStorage. URL return localhost sai khi khách dùng máy khác | Kiểm chủ đơn/trạng thái/hạn giữ chỗ; thiết kế callback public có xác minh chữ ký và xử lý kết quả riêng; cấu hình URL khả dụng; bổ sung cơ chế callback đáng tin cậy theo hợp đồng cổng thanh toán |
| F17 | Medium | `fe/src/pages/GuestPages.jsx#HomePage`; `AdminPages#AdminBaoCaoPage/AdminDoanhThuPage` | Nhãn trống/giá fallback không phản ánh dữ liệu; select báo cáo không nối state; tải nhanh nhãn 09/2026 nhưng gửi ngày form; “tăng trưởng” là số sân có doanh thu, “lượt đặt” là số giao dịch; nút Tuần chuyển local midnight sang ISO UTC làm lệch ngày | Không trình bày fallback như dữ liệu thật; nối bộ lọc đúng API; tính khoảng ngày local nhất quán; dùng tên chỉ số đúng hoặc bổ sung query sau duyệt |
| F18 | Medium | `controller/QuanLySanController.java#layMaTranLich`; `fe/src/components/StaffScheduleMatrix.jsx` | Giá trong ô đã có đơn lấy GiaSan hiện tại thay vì DatSan.gia snapshot; đổi bảng giá khiến chi tiết hiển thị sai giá đã đặt; không refresh tự động sau thay đổi ngoài page | Trả giá booking cho ô có đơn, tách giá đề xuất; xác định refresh/polling nếu cần cập nhật liên tục |
| F19 | Medium | `controller/NhanVienDatSanController.java#checkIn`; `fe/src/pages/StaffOnsitePages.jsx#PosCheckinPage` | Check-in chỉ cần DA_XAC_NHAN, có thể check-in đơn tương lai xa/quá khứ. Text QR nhưng chỉ input tìm kiếm, không có camera/giải mã QR | Xác nhận quy tắc thời gian check-in và kiểm tại BE; nói rõ khả năng nhận text từ thiết bị quét, không mặc định đã có scanner camera |
| F20 | Low | `fe/src/pages/StaffPages.jsx`; `pages/DangKy.jsx`; `components/QuanLySan.jsx`, `ProductForm.jsx`; `api/productApi.js`, `authApi.js#quenMatKhau`; `util/JwtUtil.java`; DTO DangNhapRequest/Response | Các màn hình/module không vào App; StaffPages gọi 3 URL không tồn tại; DangKy import react-router-dom không khai báo dependency. JwtUtil dùng khóa random nhưng không caller; DTO login rời, Auth dùng Map; một số import Java trùng | Xác nhận có giữ code demo/legacy không rồi loại bỏ/tích hợp trong thay đổi riêng; không tự xóa; wrapper còn giữ phải khớp API |
| F21 | Medium | `fe/src/pages/StaffOnsitePages.jsx#WalkinBookingPage.submit`; `controller/NhanVienDatSanController.java#taoDonTaiQuay` | UI cho chọn VNPay nhưng tạo đơn xong không gọi create link/redirect; BE tự tạo payment chờ và booking DA_XAC_NHAN | Xác định phương thức tại quầy được hỗ trợ; nếu VNPay được yêu cầu thì nối link/thời hạn/trạng thái đầy đủ |
| F22 | Medium | `fe/src/App.jsx#ROLE_PERMISSIONS/renderPage`; `components/navigationConfig.js`; `controller/ThanhToanController#danhSachGiaoDich/xacNhanThanhToan` | API cho NHAN_VIEN_SAN xác nhận tiền mặt/chuyển khoản nhưng page thanh-toan chỉ admin; POS không gọi thu tiền | Xác nhận phạm vi thu tiền onsite; nếu cần thì tích hợp route/navigation và hành động thích hợp sau duyệt |
| F23 | High | `service/VNPayService.java#handleReturn/createPaymentUrl`; `entity/ThanhToan.java#maGiaoDichVnpay` | Thiếu @Transactional cho đổi payment+booking; không kiểm vnp_Amount/merchant/transaction status cùng state đơn; không guard replay hay đơn hủy/giữ chỗ hết hạn. Callback thành công có thể xác nhận lại đơn hết hạn đã nhường slot; retry create thay txnRef cũ; không unique txnRef | Transaction/idempotency, tham chiếu giao dịch ổn định/unique, kiểm tổng tiền và state; xử lý thanh toán muộn; test callback lặp/thất bại/đơn hủy |
| F24 | Medium | `service/MinioService.java#upload/download`; `ProductService#create/downloadImage/getImageContentType`; `ProductController#getImage`; `AdminDashboardController#exportExcelReport/exportPdfReport` | Upload trả full URL nhưng download nhận objectName: ProductController truyền imageUrl nguyên vào getObject. Báo cáo/ảnh trả URL thẳng, không set bucket policy hoặc dùng presigned trong luồng gọi, nên bucket private có thể 403. Upload ảnh không kiểm content thực; window.open sau await có thể bị chặn popup | Lưu/tách object key; chọn authenticated download hoặc presigned theo quyền; xác minh bucket policy thực tế; validate ảnh; xử lý mở/tải file phù hợp |
| F25 | Medium | `fe/src/api/http.js#http`; `staffApi.js` interceptor; `productApi.js`; `config/CorsConfig.java`, `SecurityConfig#corsConfigurationSource`; `fe/.env`, Dockerfile, .gitlab-ci.yml | BASE_URL mặc định không thống nhất; JSON.parse phien ở http/interceptor có thể throw trước error handling; http bỏ chi tiết fields và nuốt lỗi parse response 200 thành {}; không xử lý 401 clear session; productApi thiếu token; CORS cấu hình phân tán | Chuẩn hóa API client/env, parse storage an toàn, giữ status/fields, định nghĩa 401/403; kiểm preflight origin triển khai và HTTPS. Local .env hiện override fallback nên không khẳng định lỗi LAN đang xảy ra |
| F26 | Medium | `fe/.eslintrc.cjs`, `src/api/authApi.js`, `src/components/ui.jsx`, các page; `fe/.gitlab-ci.yml`; `src/test` không tồn tại | Lint fail chủ yếu whitespace NBSP, thiếu prop-types/import thừa và hook deps; FE pipeline không gate lint/test; BE test không có bài nào | Sửa lint trong thay đổi riêng; thêm test nghiệp vụ có ý nghĩa và đưa vào CI; không dùng build pass thay bằng chứng E2E |
| F27 | Low | `config/SecurityConfig.java`; `application.yml#h2`; `controller/HealthController.java#health`; `config/OpenApiConfig.java` | H2 console public và frameOptions tắt toàn bộ dù datasource PostgreSQL; Swagger public; health luôn UP, không kiểm dependencies; tài liệu OpenAPI còn mô tả Product CRUD | Giới hạn theo profile/môi trường và nhu cầu; health kiểm dependency khi cần; cập nhật mô tả API |

### Kiểm tra Database chi tiết

- Mapping có 9 bảng: `nguoi_dung`, `cum_san`, `san`, `khung_gio`, `gia_san`, `dat_san`, `thanh_toan`, `san_yeu_thich`, `products`. PK Long/IDENTITY; các FK quan trọng booking khách/sân/giờ và payment booking đều nullable=false.
- NguoiDung unique email/username; GiaSan unique sân+giờ+loại ngày; YeuThich unique user+sân. Chưa có ràng buộc chống booking đồng thời hoặc transaction reference duplicate. loai_ngay nullable khiến unique không đảm bảo chống mọi trường hợp null; controller giá hiện chuẩn hóa null thành TAT_CA nhưng dữ liệu cũ/chèn ngoài API chưa được kiểm.
- ActiveStatusConverter đảo true “đang hoạt động” thành false `khoa_tai_khoan`; alias getKhoaTaiKhoan đảo ngược tương ứng. Mapping này nhất quán trong source, chưa đối chiếu cột dữ liệu thực. Không kết luận bool đang lưu sai.
- Snapshot DatSan.gia là BigDecimal precision=12, scale=0; payment soTien/tongTien cùng kiểu. Products dùng Double và là module demo riêng. SoDienThoai 15, username 50, role 20 và enum vai trò hiện tại phù hợp độ dài; validation request chưa khớp hết các cột.
- Quan hệ hai chiều có cascade ALL ở San/KhungGio/DatSan/CumSan; rủi ro xóa lịch sử F11. DTO/Map đang ngăn trả graph Entity vòng lặp cho sân/giá/đơn/nhân viên; comment trong sanApi nói trả San thô không còn đúng.
- Transaction đã có cho online booking, staff approve/reject/checkin/walkin, favorite và confirm payment. Chưa có transaction bao trọn VNPay tạo/return hoặc customer cancel+payment. Entity @Data trên quan hệ hai chiều còn nguy cơ recursion khi equals/hashCode/toString được dùng; chưa thấy lỗi runtime từ việc đó.
- Không có migration/version schema; `ddl-auto: update` không phải bằng chứng mapping khớp PostgreSQL hiện hữu. Cần read-only schema inspection có môi trường phù hợp để xác minh tên cột, kiểu, null, FK, unique và dữ liệu cũ. Báo cáo này không tạo/xóa/sửa schema.

## 10. Kết quả xóa nút “Đặt sân” trang chủ

Không có ảnh trong attachment được cung cấp lần này; xác định mục tiêu từ nhãn đúng “Đặt sân” và CSS `.nav-pill.active` nền xanh nhạt `rgba(0,168,120,0.08)`, khác các CTA “Đặt sân ngay” trong nội dung. Nếu ảnh gốc chỉ một vị trí khác, cần đối chiếu ảnh trước khi xác nhận khớp ảnh tuyệt đối.

Đã ẩn riêng mục điều hướng header này khi page=`home`, cho cả khách chưa đăng nhập và khách hàng:

- [fe/src/App.jsx](<D:/Năm 3/RaiWay99/QuanLiSanBong/fe/src/App.jsx:316>): truyền hideBookingNav chỉ cho trang chủ guest.
- [fe/src/components/GuestLayout.jsx](<D:/Năm 3/RaiWay99/QuanLiSanBong/fe/src/components/GuestLayout.jsx:4>): render nút có điều kiện; mặc định vẫn hiển thị ở trang khác; khai báo kiểu prop bằng dependency PropTypes có sẵn.
- [fe/src/components/Layout.jsx](<D:/Năm 3/RaiWay99/QuanLiSanBong/fe/src/components/Layout.jsx:31>): lọc item home riêng khi đang ở home của customer.

Giữ nguyên CTA hero/card sân, trang chi tiết, API đặt sân, nav lịch sử/yêu thích và các nút đặt sân ở trang khác. Không sửa CSS, kích thước header, breakpoint responsive, Backend hoặc DB. Có 9 assert render server xác nhận scope; chưa kiểm tra bằng browser/screenshot ở các viewport, nên chưa chứng nhận bố cục pixel hoặc đối chiếu ảnh.

## 11. Kết quả build/lint/test

| Lệnh/kiểm tra | Kết quả | Giới hạn |
|---|---|---|
| `cd fe; npm run build` trước sửa | PASS, Vite 113 modules | Không chứng minh API hoặc DB hoạt động |
| `cd fe; npm run lint` trước sửa | FAIL: 241 errors, 2 warnings | Không tự sửa các lỗi ngoài phạm vi |
| `mvn -o test` qua PATH | Không chạy: mvn chưa trên PATH | Sau đó đã tìm được Maven của IntelliJ |
| `& 'C:/Program Files/JetBrains/IntelliJ IDEA 2026.1/plugins/maven/lib/maven3/bin/mvn.cmd' -o test` | BUILD SUCCESS, No tests to run | Lần này classes đã có, chưa ép compile lại |
| Cùng Maven: `-o clean test` | BUILD SUCCESS; compile mới 76 source; No tests to run | Không khởi động app và không gọi DB; output wrapper có “Access is denied.” nhưng Maven hoàn tất exit 0; không coi là pass nghiệp vụ |
| Node inline dùng esbuild + react-dom/server + node:assert | PASS 9 assertions | Render Guest home/ngoài home/default và Customer home/history/favorites/field-detail/profile; kiểm nav còn lại; không browser E2E |
| `cd fe; npm run build` sau thay đổi cuối | PASS; 113 modules, CSS 78.88 kB, JS khoảng 349.08 kB | Không đổi dependencies |
| `cd fe; npm run lint` sau thay đổi cuối | FAIL: 239 errors, 2 warnings | Hai lỗi prop-types GuestLayout cũ được đáp ứng khi khai báo contract prop mới; không có lỗi lint mới trong 3 file sửa |
| `git -C fe diff --check` | PASS | Kiểm whitespace diff, không kiểm nghiệp vụ |

Frontend không khai báo script test; Backend không có source test. Không gọi VNPay sandbox, MinIO, PostgreSQL từ xa, không thực hiện upload/export/mutation qua HTTP. Báo cáo không sử dụng artifact target cũ làm bằng chứng chạy hiện tại.

## 12. Kế hoạch hoàn thiện theo ưu tiên

1. Sau phê duyệt, xử lý Critical secrets/JWT rồi High khóa tài khoản và quyền VNPay/callback. Chuẩn bị môi trường test tách Database thật, seed giới hạn profile, cấu hình URL rõ ràng.
2. Sửa tongTien trên các nhánh tạo payment; transaction, idempotency và đồng bộ booking/payment; kiểm đặt đồng thời, callback muộn/lặp, hủy sau trả tiền.
3. Thống nhất slot hiệu lực/overlap/ngày giờ/trạng thái sân; chặn hard delete lịch sử và kiểm schema/FK bằng truy vấn read-only trước khi đề xuất migration.
4. Sửa lưu cumSanId, validation user/role và load profile; xác nhận scope phân công nhân viên, thu tiền onsite và thay đổi lịch/hoàn thành đơn trước khi phát triển.
5. Sửa loading/error/retry, dữ liệu hard-code và bộ lọc báo cáo; xử lý MinIO object key/quyền tải file, URL/CORS và thống nhất client.
6. Sửa lint, xác định giữ/bỏ legacy trong thay đổi riêng; đưa test vào CI. Chạy E2E từng vai trò với dữ liệu test: đăng ký → login → xem lịch → đặt → chống trùng → duyệt → trả tiền → check-in → hóa đơn → thống kê, kèm các nhánh lỗi/hủy/quyền.

Không triển khai kế hoạch hoặc tự thêm chức năng trong lần kiểm tra này. Code chỉ thay đổi cho việc ẩn nút header trang chủ; báo cáo là artifact mới để bạn xem xét.
