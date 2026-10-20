# Lịch sử đặt sân Customer — 13/10/2026

Đã bổ sung mục Lịch sử đặt sân vào thanh điều hướng Desktop và menu Mobile cho Customer đăng nhập. Giữ trang lịch sử hiện có, không tạo trang trùng hoặc đổi thiết kế.

## Routing và đăng nhập

- Đường dẫn chính: /lich-su-dat-san. /lich-su vẫn hoạt động như alias.
- Nút lịch sử trên trang kết quả VNPay đi qua cùng routing.
- Guest mở lịch sử thấy yêu cầu đăng nhập, không gọi API lịch sử.
- Chọn Đăng nhập từ nội dung hoặc Header khi đang ở lịch sử lưu đích history trong sessionStorage. Sau đăng nhập Customer thành công, quay lại /lich-su-dat-san. Giá trị đích được giới hạn history; không nhận redirect URL bên ngoài. Các vai trò khác vẫn vào trang theo quyền hiện có.

## API và dữ liệu

GET /api/dat-san/cua-toi dùng Bearer JWT, KHACH_HANG. Backend lấy ID từ principal và query findByKhachHangIdOrderByNgayTaoDesc; Frontend không gửi ID khách hàng. Bổ sung trangThaiThanhToan và canXuLyHoanTien từ giao dịch gần nhất trong DTO lịch sử, transaction readOnly. Không thay schema hoặc nghiệp vụ thanh toán.

Trang hiển thị mã đơn, tên sân, ngày, giờ bắt đầu/kết thúc, giá snapshot, trạng thái đặt sân, trạng thái thanh toán. Giao dịch cần kiểm tra hoàn tiền được hiển thị đúng cờ. Nếu chưa có giao dịch, hiển thị Chưa có giao dịch, không suy đoán đã thanh toán. Giữ làm mới, hủy đơn, tiếp tục thanh toán và xem hóa đơn.

## Kiểm thử

- FE: ESLint 0 lỗi, Vite build PASS, 22/22 test PASS.
- BE: Maven offline package BUILD SUCCESS, 46/46 test PASS.
- Test HTTP H2 với hai Customer xác nhận danh sách chỉ có đơn chủ phiên, chứa trạng thái thanh toán; anonymous bị từ chối.
- Browser: kiểm tra menu Customer Desktop/Mobile bằng fixture chỉ render component thật với tên QA, không giả lập dữ liệu đơn hoặc ghi Database. Mục mới có href đúng và trạng thái active. Nhấn link Mobile mở route thật, Guest được yêu cầu đăng nhập rồi chuyển tới form đăng nhập.
- Fixture giao diện đã xóa sau kiểm thử. Ảnh menu QA nằm trong thư mục visualizations.
- Chưa kiểm thử đăng nhập bằng tài khoản Customer thật rồi xem lịch sử PostgreSQL và luồng đăng nhập thành công trên trình duyệt. Không sử dụng mật khẩu thật hoặc tạo tài khoản/đơn trong Database thật để kiểm thử.

Backend IntelliJ 8084 đang chạy cần restart để nạp DTO mới. Frontend Vite 3004 tự nạp thay đổi. Không commit/push và không thay credential/Database.

## File thay đổi trong yêu cầu này

- [fe/src/components/PublicLayout.jsx](D:/Năm 3/RaiWay99/QuanLiSanBong/fe/src/components/PublicLayout.jsx)
- [fe/src/App.jsx](D:/Năm 3/RaiWay99/QuanLiSanBong/fe/src/App.jsx)
- [fe/src/pages/GuestPages.jsx](D:/Năm 3/RaiWay99/QuanLiSanBong/fe/src/pages/GuestPages.jsx)
- [fe/src/pages/PaymentResultPage.jsx](D:/Năm 3/RaiWay99/QuanLiSanBong/fe/src/pages/PaymentResultPage.jsx)
- [fe/tests/render.test.cjs](D:/Năm 3/RaiWay99/QuanLiSanBong/fe/tests/render.test.cjs)
- [src/main/java/com/example/becicd/dto/DatSanDTO.java](D:/Năm 3/RaiWay99/QuanLiSanBong/src/main/java/com/example/becicd/dto/DatSanDTO.java)
- [src/main/java/com/example/becicd/controller/DatSanController.java](D:/Năm 3/RaiWay99/QuanLiSanBong/src/main/java/com/example/becicd/controller/DatSanController.java)
- [src/test/java/com/example/becicd/SystemHttpIntegrationTest.java](D:/Năm 3/RaiWay99/QuanLiSanBong/src/test/java/com/example/becicd/SystemHttpIntegrationTest.java)
