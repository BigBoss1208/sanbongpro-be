# KỊCH BẢN THUYẾT TRÌNH SÂN BÓNG PRO — KHOẢNG 8–10 PHÚT

Đây là lời nói mẫu. Thay “nhóm em” bằng cách xưng hô và phạm vi công việc đúng với phân công thực tế. Các mục dưới đây có thể dùng để làm 10 slide; chưa phải file PowerPoint.

## Slide 1 — Giới thiệu đề tài (30 giây)

“Em xin trình bày đề tài website Quản Lý Sân Bóng, tên giao diện là SÂN BÓNG PRO. Website hỗ trợ khách hàng tìm sân, xem lịch và đặt sân; đồng thời hỗ trợ nhân viên và quản trị viên vận hành lịch đặt, giá và thanh toán. Mục tiêu chính là giảm sai sót khi quản lý thủ công, đặc biệt là đặt trùng giờ và xử lý đơn không đúng phạm vi.”

Hiển thị: ảnh trang chủ. Không dùng ảnh để khẳng định đó là sân thật đang có trên hệ thống.

## Slide 2 — Công nghệ và kiến trúc (45 giây)

“Frontend sử dụng React và Vite. Backend sử dụng Spring Boot, Spring Security và Spring Data JPA; dữ liệu chính lưu trong PostgreSQL. Giao diện gọi REST API và nhận JSON. Backend kiểm tra quyền, xử lý nghiệp vụ và truy cập database qua Repository. MinIO phục vụ lưu file, còn source có các phương thức thanh toán gồm tiền mặt, chuyển khoản và tích hợp VNPay.”

“Trong source hiện tại, một phần nghiệp vụ nằm trong Controller; các phần dùng chung được gom vào BookingGuard và những Service liên quan. Nhóm chưa tách toàn bộ nghiệp vụ sang Service.”

Hiển thị: sơ đồ React → HTTP/JWT → Spring Controller/Service → Repository → PostgreSQL.

## Slide 3 — Người dùng và chức năng (45 giây)

“Có khách chưa đăng nhập và bốn role tài khoản. Khách hàng có thể đặt sân, xem lịch sử và lưu yêu thích. Nhân viên Page xử lý yêu cầu và lịch trong cụm được phân công. Nhân viên Sân làm các công việc tại sân như đặt tại quầy và check-in. Admin quản lý danh mục sân, cụm, giờ, giá, tài khoản và báo cáo.”

“Phân quyền được kiểm tra ở Backend; việc ẩn nút hoặc chặn trang ở Frontend chỉ hỗ trợ trải nghiệm.”

Hiển thị: bảng role trong báo cáo chính.

## Slide 4 — Những phần giao diện đã hoàn thiện (45 giây)

“Trang chủ có Hero sân vận động lớn, form tìm kiếm được đưa xuống nền trắng để dễ thao tác. Danh sách lấy từ API, hỗ trợ tìm kiếm tiếng Việt có hoặc không dấu, lọc cụm và loại sân, sắp xếp giá. Phần Login và Register dùng AuthLayout chung: form trắng ở giữa, background sân vận động đỏ và thiết kế responsive.”

“Khi chỉnh giao diện, các trường đăng ký, validation, API đăng nhập và cách lưu phiên được giữ nguyên. Màn hình còn có loading, lỗi và toggle mật khẩu.”

Hiển thị: ảnh Login/Register Desktop và Mobile đã kiểm tra. Nếu Backend chưa chạy, trình bày ảnh và UI thay vì giả vờ thao tác đặt sân thành công.

## Slide 5 — Luồng từ giao diện đến API (60 giây)

“Ví dụ khi tạo đơn đặt sân, Frontend gửi sanId, khungGioId, ngayDat và phương thức thanh toán đến POST /api/dat-san. Client chung gắn Bearer token và xử lý lỗi HTTP. Backend không tin hoàn toàn dữ liệu trên giao diện: phải kiểm tra tài khoản, sân, ngày giờ, lịch đã bị chiếm và giá từ database.”

“Giá được lưu vào đơn tại thời điểm đặt. Vì vậy quản trị viên thay bảng giá cho những đơn sau sẽ không làm đổi giá của đơn cũ.”

Mở: `fe/src/api/datSanApi.js`, sau đó mở đoạn `datSan.setGia(giaSan.getGia())` trong `DatSanController.java`. Chỉ nói đến các dòng liên quan, không đọc toàn bộ Controller.

## Slide 6 — Chống đặt trùng (90 giây)

“Chống đặt trùng là phần trọng tâm. Nếu hai người cùng gửi yêu cầu, chỉ kiểm tra bằng JavaScript sẽ chưa đủ. Backend dùng transaction và truy vấn có khóa pessimistic trên sân. Request phải lấy khóa rồi mới kiểm tra lịch và lưu đơn.”

“BookingGuard kiểm tra thời gian giao nhau. Điều kiện là giờ bắt đầu của A trước giờ kết thúc của B, đồng thời giờ bắt đầu của B trước giờ kết thúc của A. Ví dụ 18 giờ đến 19 giờ sẽ trùng với 18 giờ 30 đến 19 giờ 30, nhưng không trùng với 19 giờ đến 20 giờ.”

“Đơn giữ chỗ online có hạn 15 phút. Guard chỉ coi giữ chỗ còn hiệu lực là đang chiếm lịch. Khi đơn hủy hoặc giữ chỗ hết hiệu lực, logic kiểm tra lịch không tiếp tục giữ khoảng đó theo trạng thái này.”

Mở: `SanRepository.findLockedById`, `BookingGuard.overlaps`, `BookingGuard.occupies` và annotation `@Transactional` của tạo đơn.

Không nói “toàn bộ hệ thống không thể có race condition”; chỉ trình bày cơ chế và phạm vi booking đã kiểm thử.

## Slide 7 — Bảo mật và phạm vi nhân viên (60 giây)

“Mật khẩu tài khoản được băm bằng BCrypt. Khi đăng nhập đúng, Backend cấp JWT; những API cần xác thực sẽ kiểm tra token. Bộ lọc còn tải lại người dùng để kiểm tra trạng thái khóa và lấy quyền hiện tại.”

“Nhân viên chỉ xử lý sân thuộc cụm đã được phân công. Backend đối chiếu ID cụm của nhân viên với ID cụm của sân, nên việc tự sửa ID trong URL không vượt được kiểm tra này. Check-in yêu cầu role phù hợp, đơn đã xác nhận và thời gian từ 30 phút trước bắt đầu đến giờ kết thúc.”

Mở: `SecurityConfig`, `BookingGuard.requireStaffScope` và đoạn thời gian trong `NhanVienDatSanController.checkIn`.

## Slide 8 — Lỗi đã xử lý và migration (60 giây)

“Trong quá trình hoàn thiện, Backend có lỗi không nhận JWT secret vì profile local chưa được chọn đúng. Nhóm xử lý cách nạp cấu hình local và tách thông báo lỗi key thiếu, key ngắn hoặc expiration không hợp lệ, vẫn giữ kiểm tra bảo mật.”

“Một lỗi khác là Entity ThanhToan có cờ cần xử lý hoàn tiền, nhưng PostgreSQL thiếu cột can_xu_ly_hoan_tien. Sau khi kiểm tra đúng database và backup, nhóm chạy migration được duyệt để thêm BOOLEAN NOT NULL DEFAULT FALSE. Sau chạy, ba thanh toán cùng dữ liệu đặt sân liên quan giữ nguyên. Hibernate vẫn dùng validate.”

Hiển thị: một dòng ALTER TABLE và kết quả kiểm tra đã che dữ liệu cá nhân. Không mở .env hoặc màn hình credentials.

## Slide 9 — Kiểm thử và giới hạn (60 giây)

“Lần kiểm tra gần nhất, Backend build thành công với 41 test pass. Frontend có 19 test pass, lint và build qua CLI local thành công. Giao diện Login/Register được kiểm tra từ 320 đến 1440 pixel, gồm validation, loading và thông báo lỗi.”

“Nhóm phân biệt kiểm thử trên môi trường cô lập với môi trường thật. Sau migration, PostgreSQL đã qua kiểm tra schema, nhưng startup hiện còn dừng vì thiếu MINIO_ENDPOINT. VNPay thật, upload MinIO thật và một số chức năng liên quan dịch vụ ngoài chưa được nghiệm thu đầy đủ.”

“Việc đưa bản cập nhật lên main đang dừng vì phát hiện credentials trong lịch sử Backend. Cần xử lý các credential đã lộ và thống nhất cách xử lý lịch sử trước khi tiếp tục công bố.”

Không đọc hoặc hiển thị giá trị secret. Không báo GitLab CI pass, push main thành công hoặc triển khai production khi chưa có bằng chứng.

## Slide 10 — Kết luận (30 giây)

“Kết quả chính của dự án là giao diện đặt sân gắn với API và dữ liệu, quản lý lịch cùng trạng thái, phân quyền theo role và cụm, cùng cơ chế chống đặt trùng. Qua quá trình làm, nhóm học được cách phối hợp Frontend và Backend, kiểm thử nghiệp vụ và kiểm soát thay đổi database. Hướng tiếp theo là hoàn thiện cấu hình dịch vụ thật, nghiệm thu thanh toán và triển khai sau khi xử lý vấn đề bảo mật còn lại.”

## Chuẩn bị trước buổi trình bày

1. Xác định phần việc cá nhân để trình bày đúng; dùng báo cáo chính giải thích phần code của mình.
2. Chuẩn bị ảnh UI và các đoạn code ở slide 5–8. Dùng ảnh dự phòng khi môi trường demo chưa chạy đủ dịch vụ.
3. Nếu cần demo nghiệp vụ trực tiếp, phải xử lý cấu hình MinIO và xác minh Backend/API trước buổi trình bày trong một nhiệm vụ được cho phép riêng. Tài liệu này không sửa môi trường đó.
4. Chỉ dùng tài khoản/dữ liệu demo được phép; không gọi giao dịch thanh toán thật trong phần trình diễn.
5. Luyện ba câu trả lời: vì sao cần khóa dữ liệu, vì sao lưu snapshot giá, vì sao phân quyền Backend mới quyết định quyền xử lý.
